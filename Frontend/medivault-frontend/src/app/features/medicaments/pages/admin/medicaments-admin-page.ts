import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Icon } from '../../../../shared/icon';
import { CountUp } from '../../../../shared/count-up';
import { Hero } from '../../components/hero';
import { SidePanel } from '../../components/side-panel';
import { QuotaBar } from '../../components/quota-bar';
import { CategorieBadge } from '../../components/categorie-badge';
import {
  Attribution, CATEGORIES, Categorie, MedicamentSensible, ResultatLot, VenteSensible, categorieInfo, libelleMois
} from '../../models/sensible.model';
import { SensibleService } from '../../services/sensible.service';

type Onglet = 'catalogue' | 'quotas' | 'ventes';

const auMoinsUn = (c: AbstractControl): ValidationErrors | null => (c.value?.length ? null : { vide: true });
const normaliser = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

// Espace administrateur : catalogue des médicaments sensibles, quotas des pharmacies et ventes.
// L'admin gère tout (CRUD) ; les formulaires sont des Reactive Forms avec validation.
@Component({
  selector: 'mv-medicaments-admin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, Icon, CountUp, Hero, SidePanel, QuotaBar, CategorieBadge],
  templateUrl: './medicaments-admin-page.html'
})
export class MedicamentsAdminPage {
  protected s = inject(SensibleService);
  private fb = inject(FormBuilder);

  protected readonly categories = CATEGORIES;
  protected readonly libelleMois = libelleMois;
  protected readonly categorieInfo = categorieInfo;

  protected onglet = signal<Onglet>('catalogue');
  protected filtreCat = signal<Categorie | null>(null);
  protected recherche = signal('');
  protected pret = signal(false);
  protected toast = signal<{ texte: string; ok: boolean } | null>(null);

  // ---------- Panneaux ----------
  protected detailMed = signal<MedicamentSensible | null>(null);
  protected fiche = signal<number | null>(null);
  protected detailVente = signal<VenteSensible | null>(null);
  protected formMedOuvert = signal(false);
  protected enEdition = signal<number | null>(null);
  protected lotOuvert = signal(false);
  protected quotaEdite = signal<Attribution | null>(null);
  protected aSupprimer = signal<{ type: 'med' | 'quota'; id: number; libelle: string } | null>(null);
  protected envoi = signal(false);
  protected erreurForm = signal('');
  protected resultatLot = signal<ResultatLot | null>(null);
  protected recherchePharma = signal('');

  // ---------- Formulaires (Reactive Forms) ----------
  protected formMed = this.fb.nonNullable.group({
    nomCommercial: ['', [Validators.required, Validators.minLength(2)]],
    dci: ['', [Validators.required, Validators.minLength(2)]],
    categorie: ['' as Categorie | '', Validators.required],
    description: ['', [Validators.required, Validators.minLength(10)]],
    precautions: ['', [Validators.required, Validators.minLength(5)]]
  });
  protected formLot = this.fb.group({
    medicamentId: [null as number | null, Validators.required],
    mois: [this.s.moisCourant, Validators.required],
    quantite: [null as number | null, [Validators.required, Validators.min(1)]],
    pharmacieIds: [[] as number[], auMoinsUn]
  });
  protected formQuota = this.fb.nonNullable.group({ quantite: [1, [Validators.required, Validators.min(1)]] });
  private lotValeurs = toSignal(this.formLot.valueChanges, { initialValue: this.formLot.getRawValue() });

  // ---------- Données affichées ----------
  protected comptes = computed(() => CATEGORIES.map(c => ({ ...c, nb: this.s.medicaments().filter(m => m.categorie === c.cle).length })));

  protected catalogue = computed(() => {
    const q = normaliser(this.recherche()), c = this.filtreCat();
    return this.s.medicaments()
      .filter(m => (!c || m.categorie === c) && (!q || normaliser(`${m.nomCommercial} ${m.dci}`).includes(q)))
      .map(m => {
        const att = this.s.attributionsDuMois().filter(a => a.medicamentId === m.id);
        return { m, nbPharmacies: att.length, vendus: att.reduce((t, a) => t + a.quantiteVendue, 0) };
      });
  });

  protected quotasParPharmacie = computed(() =>
    this.s.pharmacies.map(p => {
      const quotas = this.s.attributionsDuMois().filter(a => a.pharmacieId === p.id);
      return {
        p, quotas, attribue: quotas.reduce((t, a) => t + a.quantiteAttribuee, 0), vendu: quotas.reduce((t, a) => t + a.quantiteVendue, 0),
        avert: quotas.reduce((t, a) => t + a.avertissements, 0), statut: this.s.statutPharmacie(p.id)
      };
    }).filter(l => l.quotas.length)
  );

  protected fPharma = signal(0);
  protected fMed = signal(0);
  protected fMois = signal(this.s.moisCourant);
  protected ventesFiltrees = computed(() =>
    this.s.ventes().filter(v =>
      (!this.fPharma() || v.pharmacieId === this.fPharma()) &&
      (!this.fMed() || v.medicamentId === this.fMed()) &&
      (!this.fMois() || v.date.startsWith(this.fMois()))
    )
  );

  protected pharmaciesDuMed = computed(() => {
    const m = this.detailMed();
    return m ? this.s.attributionsDuMois().filter(a => a.medicamentId === m.id).map(a => ({ a, p: this.s.pharmacie(a.pharmacieId)! })) : [];
  });

  protected fichePharmacie = computed(() => {
    const id = this.fiche();
    if (id === null) return null;
    const quotas = this.s.attributionsDuMois().filter(a => a.pharmacieId === id).map(a => ({ a, m: this.s.medicament(a.medicamentId)! }));
    const attribue = quotas.reduce((t, q) => t + q.a.quantiteAttribuee, 0), vendu = quotas.reduce((t, q) => t + q.a.quantiteVendue, 0);
    return {
      p: this.s.pharmacie(id)!, statut: this.s.statutPharmacie(id), quotas, attribue, vendu,
      taux: attribue ? Math.round((vendu / attribue) * 100) : 0, avert: quotas.reduce((t, q) => t + q.a.avertissements, 0),
      ventes: this.s.ventes().filter(v => v.pharmacieId === id).slice(0, 6)
    };
  });

  protected pharmaciesFiltrees = computed(() => {
    const q = normaliser(this.recherchePharma());
    return this.s.pharmacies.filter(p => !q || normaliser(`${p.nom} ${p.ville}`).includes(q));
  });
  protected resume = computed(() => {
    const v = this.lotValeurs();
    const n = v.pharmacieIds?.length ?? 0, med = this.s.medicament(Number(v.medicamentId));
    if (!n || !v.quantite || !med || !v.mois) return '';
    return `${n} pharmacie${n > 1 ? 's' : ''} recevr${n > 1 ? 'ont' : 'a'} ${v.quantite} ${med.nomCommercial} en ${libelleMois(v.mois)}`;
  });
  protected nomMedLot = computed(() => this.s.medicament(Number(this.lotValeurs().medicamentId))?.nomCommercial ?? '');

  constructor() {
    this.s.charger().subscribe();
    setTimeout(() => this.pret.set(true), 300);
  }

  // ---------- Aides d'affichage ----------
  protected msg(c: AbstractControl | null) {
    if (!c || !(c.touched || c.dirty) || !c.errors) return '';
    if (c.errors['required']) return 'Ce champ est obligatoire.';
    if (c.errors['minlength']) return `Au moins ${c.errors['minlength'].requiredLength} caractères.`;
    if (c.errors['min']) return 'La valeur doit être supérieure à 0.';
    if (c.errors['vide']) return 'Sélectionnez au moins une pharmacie.';
    return 'Valeur invalide.';
  }
  protected nbJours(date: string) { return new Date(date); }
  protected tauxVente(a: Attribution) { return a.quantiteAttribuee ? Math.round((a.quantiteVendue / a.quantiteAttribuee) * 100) : 0; }
  protected infoOrdonnance(v: VenteSensible) { return this.s.ordonnance(v.ordonnanceId); }

  private notifier(texte: string, ok = true) {
    this.toast.set({ texte, ok });
    setTimeout(() => this.toast.set(null), 3500);
  }

  // ---------- Médicament : ajout, modification ----------
  protected ouvrirAjout() {
    this.enEdition.set(null);
    this.formMed.reset({ nomCommercial: '', dci: '', categorie: '', description: '', precautions: '' });
    this.erreurForm.set('');
    this.formMedOuvert.set(true);
  }
  protected ouvrirEdition(m: MedicamentSensible) {
    this.detailMed.set(null);
    this.enEdition.set(m.id);
    this.formMed.reset({ nomCommercial: m.nomCommercial, dci: m.dci, categorie: m.categorie, description: m.description, precautions: m.precautions });
    this.erreurForm.set('');
    this.formMedOuvert.set(true);
  }
  protected choisirCategorie(c: Categorie) {
    this.formMed.controls.categorie.setValue(c);
    this.formMed.controls.categorie.markAsTouched();
  }
  protected enregistrerMed() {
    if (this.formMed.invalid) { this.formMed.markAllAsTouched(); return; }
    const v = this.formMed.getRawValue();
    const corps = { ...v, categorie: v.categorie as Categorie };
    const id = this.enEdition();
    this.envoi.set(true);
    (id === null ? this.s.creerMedicament(corps) : this.s.modifierMedicament(id, corps)).subscribe({
      next: m => {
        this.envoi.set(false);
        this.formMedOuvert.set(false);
        this.notifier(id === null ? `${m.nomCommercial} ajouté au catalogue` : `${m.nomCommercial} mis à jour`);
      },
      error: e => {
        this.envoi.set(false);
        this.erreurForm.set(e?.status === 409 ? 'Ce médicament existe déjà dans le catalogue.' : 'Enregistrement impossible. Réessayez dans un instant.');
      }
    });
  }

  // ---------- Quotas : attribution en lot ----------
  protected ouvrirLot(med?: MedicamentSensible) {
    this.detailMed.set(null);
    this.fiche.set(null);
    this.formLot.reset({ medicamentId: med?.id ?? null, mois: this.s.moisCourant, quantite: null, pharmacieIds: [] });
    this.recherchePharma.set('');
    this.resultatLot.set(null);
    this.erreurForm.set('');
    this.lotOuvert.set(true);
  }
  protected cocher(id: number) {
    const c = this.formLot.controls.pharmacieIds;
    const liste = c.value ?? [];
    c.setValue(liste.includes(id) ? liste.filter(x => x !== id) : [...liste, id]);
    c.markAsTouched();
  }
  protected estCochee(id: number) { return (this.lotValeurs().pharmacieIds ?? []).includes(id); }
  protected toutCocher() {
    const ids = this.pharmaciesFiltrees().map(p => p.id), c = this.formLot.controls.pharmacieIds;
    const tous = ids.every(id => (c.value ?? []).includes(id));
    c.setValue(tous ? (c.value ?? []).filter(x => !ids.includes(x)) : [...new Set([...(c.value ?? []), ...ids])]);
    c.markAsTouched();
  }
  protected validerLot() {
    if (this.formLot.invalid) { this.formLot.markAllAsTouched(); return; }
    const v = this.formLot.getRawValue();
    this.envoi.set(true);
    this.s.attribuerLot({ medicamentId: Number(v.medicamentId), mois: v.mois!, quantite: Number(v.quantite), pharmacieIds: v.pharmacieIds! }).subscribe({
      next: r => { this.envoi.set(false); this.resultatLot.set(r); },
      error: () => { this.envoi.set(false); this.erreurForm.set('Attribution impossible. Réessayez dans un instant.'); }
    });
  }

  // ---------- Quotas : modification d'un quota ----------
  protected ouvrirQuota(a: Attribution) {
    this.quotaEdite.set(a);
    this.formQuota.reset({ quantite: a.quantiteAttribuee });
  }
  protected enregistrerQuota() {
    const a = this.quotaEdite();
    if (!a) return;
    if (this.formQuota.invalid) { this.formQuota.markAllAsTouched(); return; }
    this.s.modifierAttribution(a.id, this.formQuota.getRawValue().quantite).subscribe(() => {
      this.quotaEdite.set(null);
      this.notifier('Quota mis à jour');
    });
  }

  // ---------- Suppression avec confirmation ----------
  protected demanderSuppression(type: 'med' | 'quota', id: number, libelle: string) { this.aSupprimer.set({ type, id, libelle }); }
  protected confirmerSuppression() {
    const x = this.aSupprimer();
    if (!x) return;
    (x.type === 'med' ? this.s.supprimerMedicament(x.id) : this.s.supprimerAttribution(x.id)).subscribe({
      next: () => { this.aSupprimer.set(null); this.detailMed.set(null); this.notifier(`${x.libelle} supprimé`); },
      error: () => { this.aSupprimer.set(null); this.notifier('Suppression impossible', false); }
    });
  }
}
