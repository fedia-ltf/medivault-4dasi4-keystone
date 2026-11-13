import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { MedicamentsScene } from '../components/medicaments-scene';
import { CountUp } from '../../../shared/count-up';
import { FORMES, Forme, Medicament } from '../models/medicament.model';
import { MedicamentService } from '../services/medicament.service';

@Component({
  selector: 'mv-medicaments',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon, MedicamentsScene, CountUp],
  templateUrl: './medicaments-page.html'
})
export class MedicamentsPage {
  protected api = inject(MedicamentService);
  protected readonly formes = FORMES;

  // ---------- Catalogue ----------
  protected liste = signal<Medicament[]>([]);
  protected chargement = signal(true);
  protected recherche = signal('');
  protected formeFiltre = signal<Forme | null>(null);

  protected filtres = computed(() => {
    const q = this.normaliser(this.recherche());
    const f = this.formeFiltre();
    return this.liste().filter(m =>
      (!f || m.forme === f) &&
      (!q || this.normaliser(`${m.nom_commercial} ${m.dci} ${m.classe_therapeutique}`).includes(q))
    );
  });

  protected stats = computed(() => {
    const l = this.liste();
    return [
      { label: 'Médicaments au catalogue', valeur: l.length, icon: 'pill' },
      { label: 'Classes thérapeutiques', valeur: new Set(l.map(m => m.classe_therapeutique)).size, icon: 'layers' },
      { label: 'Formes disponibles', valeur: new Set(l.map(m => m.forme)).size, icon: 'flask' },
      { label: 'Délivrances enregistrées', valeur: l.reduce((s, m) => s + (m.nb_delivrances ?? 0), 0), icon: 'activity' }
    ];
  });

  // ---------- Formulaire (ajout et modification) ----------
  protected formulaireOuvert = signal(false);
  /** null = ajout, sinon id du médicament en cours de modification */
  protected enEdition = signal<number | null>(null);
  protected saisie = this.vide();
  protected erreur = signal('');

  // ---------- Suppression ----------
  protected aRetirer = signal<Medicament | null>(null);
  protected motif = 'Rappel de lot';
  protected toast = signal<string | null>(null);

  constructor() {
    this.charger();
  }

  protected charger() {
    this.chargement.set(true);
    this.api.catalogue().subscribe(l => {
      this.liste.set(l);
      this.chargement.set(false);
    });
  }

  protected ouvrirAjout() {
    this.enEdition.set(null);
    this.saisie = this.vide();
    this.erreur.set('');
    this.formulaireOuvert.set(true);
  }

  // Même formulaire que l'ajout, pré-rempli avec le médicament choisi
  protected ouvrirModification(m: Medicament) {
    this.enEdition.set(m.id);
    this.saisie = {
      nom_commercial: m.nom_commercial, dci: m.dci, classe_therapeutique: m.classe_therapeutique,
      forme: m.forme, dosage: m.dosage
    };
    this.erreur.set('');
    this.formulaireOuvert.set(true);
  }

  protected enregistrer() {
    const s = this.saisie;
    if (!s.nom_commercial.trim() || !s.dci.trim() || !s.classe_therapeutique.trim() || !s.dosage.trim()) {
      this.erreur.set('Tous les champs sont obligatoires.');
      return;
    }
    const id = this.enEdition();
    const appel = id === null ? this.api.ajouter(s) : this.api.modifier(id, s);
    appel.subscribe({
      next: m => {
        this.formulaireOuvert.set(false);
        this.notifier(id === null ? `${m.nom_commercial} ajouté au catalogue` : `${m.nom_commercial} mis à jour`);
        this.charger();
      },
      error: () => this.erreur.set(
        id === null
          ? 'Le serveur a refusé ces données (400).'
          : 'Modification refusée : le medicament-service n\'expose pas encore PUT /api/medicaments/{id}.'
      )
    });
  }

  protected confirmerRetrait() {
    const m = this.aRetirer();
    if (!m) return;
    this.api.retirer(m.id, this.motif).subscribe({
      next: () => {
        this.aRetirer.set(null);
        this.notifier(`${m.nom_commercial} supprimé du catalogue`);
        this.charger();
      },
      error: () => this.notifier('Suppression impossible')
    });
  }

  protected tonForme(f: Forme) {
    return {
      COMPRIME: 'bg-aqua-50 text-aqua-700',
      GELULE: 'bg-teal-50 text-teal-700',
      SIROP: 'bg-amber-50 text-amber-700',
      INJECTABLE: 'bg-rose-50 text-rose-700',
      POMMADE: 'bg-violet-50 text-violet-700'
    }[f];
  }

  private notifier(msg: string) {
    this.toast.set(msg);
    setTimeout(() => this.toast.set(null), 3200);
  }

  private normaliser(s: string) {
    return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
  }

  private vide() {
    return { nom_commercial: '', dci: '', classe_therapeutique: '', forme: 'COMPRIME' as Forme, dosage: '' };
  }
}
