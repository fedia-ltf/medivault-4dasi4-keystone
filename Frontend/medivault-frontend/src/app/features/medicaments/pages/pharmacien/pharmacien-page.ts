import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Icon } from '../../../../shared/icon';
import { CountUp } from '../../../../shared/count-up';
import { Hero } from '../../components/hero';
import { QuotaBar } from '../../components/quota-bar';
import { CategorieBadge } from '../../components/categorie-badge';
import { PillIcon } from '../../components/pill-icon';
import { EtapeVente, ResultatVente } from '../../models/sensible.model';
import { SensibleService } from '../../services/sensible.service';

// Espace du pharmacien : il déclare ses ventes de médicaments sensibles et suit ses quotas du mois.
// Il ne crée ni ne modifie aucun médicament ni quota.
@Component({
  selector: 'mv-pharmacien',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, Icon, CountUp, Hero, QuotaBar, CategorieBadge, PillIcon],
  templateUrl: './pharmacien-page.html'
})
export class PharmacienPage {
  protected s = inject(SensibleService);
  private fb = inject(FormBuilder);

  protected pret = signal(false);
  protected envoi = signal(false);
  protected resultat = signal<ResultatVente | null>(null);
  protected etapes = signal<EtapeVente[]>([]);

  protected form = this.fb.group({
    ordonnanceCode: ['', [Validators.required, Validators.pattern(/^ORD-[A-Za-z0-9-]{3,}$/i)]],
    medicamentId: [null as number | null, Validators.required],
    quantite: [1, [Validators.required, Validators.min(1)]]
  });

  // ---------- Mes données ----------
  protected statut = computed(() => this.s.statutPharmacie(this.s.pharmacieId()));
  protected pharmacie = computed(() => this.s.pharmacie(this.s.pharmacieId()));
  protected mesQuotas = computed(() =>
    this.s.attributionsDuMois()
      .filter(a => a.pharmacieId === this.s.pharmacieId())
      .map(a => ({ a, m: this.s.medicament(a.medicamentId)!, restant: Math.max(0, a.quantiteAttribuee - a.quantiteVendue), taux: a.quantiteAttribuee ? Math.round((a.quantiteVendue / a.quantiteAttribuee) * 100) : 0 }))
  );
  protected attribue = computed(() => this.mesQuotas().reduce((t, q) => t + q.a.quantiteAttribuee, 0));
  protected vendu = computed(() => this.mesQuotas().reduce((t, q) => t + q.a.quantiteVendue, 0));
  protected restant = computed(() => this.attribue() - this.vendu());
  protected tauxGlobal = computed(() => (this.attribue() ? Math.round((this.vendu() / this.attribue()) * 100) : 0));
  protected avert = computed(() => Math.max(0, ...this.mesQuotas().map(q => q.a.avertissements)));
  protected mesVentes = computed(() => this.s.ventes().filter(v => v.pharmacieId === this.s.pharmacieId() && v.date.startsWith(this.s.moisCourant)));
  protected dernieres = computed(() => this.mesVentes().slice(0, 6));
  protected aSurveiller = computed(() => [...this.mesQuotas()].sort((a, b) => b.taux - a.taux).slice(0, 4));
  protected proches = computed(() => this.mesQuotas().filter(q => q.taux > 90));

  // Ventes de la semaine en cours (lundi → dimanche), en boîtes de médicaments sensibles : calculées sur les vraies ventes de la pharmacie
  protected jours = computed(() => {
    const auj = new Date();
    const lundi = new Date(auj.getFullYear(), auj.getMonth(), auj.getDate() - ((auj.getDay() + 6) % 7));
    const mesVentes = this.s.ventes().filter(x => x.pharmacieId === this.s.pharmacieId());
    return ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map((j, i) => {
      const d = new Date(lundi.getFullYear(), lundi.getMonth(), lundi.getDate() + i);
      const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      return { j, v: mesVentes.filter(x => x.date.startsWith(iso)).reduce((t, x) => t + x.quantite, 0) };
    });
  });
  protected maxJour = computed(() => Math.max(1, ...this.jours().map(j => j.v)));

  constructor() {
    this.s.charger().subscribe();
    setTimeout(() => this.pret.set(true), 300);
  }

  protected msg(c: AbstractControl | null) {
    if (!c || !(c.touched || c.dirty) || !c.errors) return '';
    if (c.errors['required']) return 'Ce champ est obligatoire.';
    if (c.errors['pattern']) return 'Format attendu : ORD-A7F3';
    if (c.errors['min']) return 'La quantité doit être au moins 1.';
    return 'Valeur invalide.';
  }
  protected date(v: string) { return new Date(v); }
  protected codeDe(id: number) { return this.s.codeOrdonnance(id); }

  /** Vérifie les règles puis enregistre : les étapes apparaissent l'une après l'autre */
  protected declarer() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.envoi.set(true);
    this.resultat.set(null);
    this.etapes.set([]);
    this.s.declarerVente({ ordonnanceCode: v.ordonnanceCode!, medicamentId: Number(v.medicamentId), quantite: Number(v.quantite) }).subscribe(r => {
      const pas = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 520;
      let i = 0;
      const suite = () => {
        if (i < r.etapes.length) {
          this.etapes.set(r.etapes.slice(0, ++i));
          setTimeout(suite, pas);
        } else {
          this.envoi.set(false);
          this.resultat.set(r);
          if (r.ok) this.form.reset({ ordonnanceCode: '', medicamentId: null, quantite: 1 });
        }
      };
      suite();
    });
  }
}
