import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { CountUp } from '../../../shared/count-up';
import { DossiersScene } from '../components/dossiers-scene';
import {
  Consultation,
  ConsultationUpdateRequest,
  Dossier,
  FiltresConsultation,
  NouvelleConsultation,
  PageResponse
} from '../models/dossier.model';
import { DossierService } from '../services/dossier.service';

export const PATIENT_DEFAUT = 'patient-ahmed';

@Component({
  selector: 'mv-dossiers-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, DatePipe, Icon, DossiersScene, CountUp],
  templateUrl: './dossiers-page.html'
})
export class DossiersPage {
  protected readonly api = inject(DossierService);

  // ---------- Patient actif ----------
  protected readonly patientId = signal<string>(PATIENT_DEFAUT);
  protected patientIdSaisi = signal<string>(PATIENT_DEFAUT);

  // ---------- État du dossier (synthèse) ----------
  protected dossier = signal<Dossier | null>(null);
  protected chargement = signal<boolean>(true);
  protected erreur = signal<{ status: number; message: string; detail?: string } | null>(null);

  // ---------- Pagination et filtres des consultations ----------
  protected pageCourante = signal<number>(0);
  protected taillePage = signal<number>(10);
  protected totalElements = signal<number>(0);
  protected totalPages = signal<number>(0);
  protected consultationsPage = signal<Consultation[]>([]);
  protected chargementConsultations = signal<boolean>(false);

  // Filtres actifs
  protected filtreMedecinId = signal<string>('');
  protected filtreDu = signal<string>('');
  protected filtreAu = signal<string>('');
  // Filtres en cours de saisie (avant d'appuyer sur Filtrer)
  protected saisieFiltreMedecin = signal<string>('');
  protected saisieFiltredu = signal<string>('');
  protected saisieFiltreAu = signal<string>('');

  // ---------- Statistiques dérivées (sur la synthèse dossier) ----------
  protected stats = computed(() => {
    const d = this.dossier();
    const nbAllergies = d?.allergies?.length ?? 0;
    const totalConsultations = this.totalElements();

    return [
      { label: 'Groupe sanguin', texte: d?.groupeSanguin || '—', icon: 'heart', ton: 'from-rose-50 to-white text-rose-600' },
      { label: 'Allergies connues', valeur: nbAllergies, icon: 'alert', ton: 'from-amber-50 to-white text-amber-600' },
      { label: 'Consultations', valeur: totalConsultations, icon: 'stethoscope', ton: 'from-aqua-50 to-white text-aqua-600' },
      { label: 'Page', texte: totalConsultations > 0 ? `${this.pageCourante() + 1}/${this.totalPages()}` : '—', icon: 'activity', ton: 'from-sky-50 to-white text-sky-600' }
    ];
  });

  // ---------- Formulaire (Ajout et Modification) ----------
  protected formulaireOuvert = signal<boolean>(false);
  protected enEdition = signal<Consultation | null>(null);
  protected soumissionEnCours = signal<boolean>(false);
  protected erreurFormulaire = signal<string | null>(null);

  protected saisie = {
    medecinId: 'medecin-karim-001',
    motif: '',
    diagnostic: '',
    observations: '',
    analysesDemandeesStr: ''
  };

  // ---------- Modale de confirmation de suppression ----------
  protected aSupprimer = signal<Consultation | null>(null);
  protected suppressionEnCours = signal<boolean>(false);
  protected erreurSuppression = signal<string | null>(null);

  // ---------- Notifications ----------
  protected toast = signal<string | null>(null);

  // ---------- Téléchargement PDF ----------
  protected pdfEnCours = signal<boolean>(false);
  protected erreurPdf = signal<string | null>(null);

  constructor() {
    this.charger();
  }

  // =========================================================
  // CHARGEMENT
  // =========================================================

  protected charger() {
    this.chargement.set(true);
    this.erreur.set(null);
    this.pageCourante.set(0);

    this.api.getDossier(this.patientId()).subscribe({
      next: (data: Dossier) => {
        this.dossier.set(data);
        this.chargement.set(false);
        this.chargerConsultations();
      },
      error: (err: unknown) => {
        this.dossier.set(null);
        this.chargement.set(false);
        this.erreur.set(this.api.formaterErreur(err));
      }
    });
  }

  protected chargerConsultations() {
    const d = this.dossier();
    if (!d) return;

    this.chargementConsultations.set(true);

    const filtres: FiltresConsultation = {};
    const med = this.filtreMedecinId().trim();
    const du  = this.filtreDu().trim();
    const au  = this.filtreAu().trim();
    if (med) filtres.medecinId = med;
    if (du)  filtres.du = du;
    if (au)  filtres.au = au;

    this.api.listerConsultations(d.id, {
      page: this.pageCourante(),
      size: this.taillePage(),
      ...filtres
    }).subscribe({
      next: (page: PageResponse<Consultation>) => {
        this.consultationsPage.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.chargementConsultations.set(false);
      },
      error: () => {
        this.chargementConsultations.set(false);
      }
    });
  }

  // =========================================================
  // NAVIGATION PATIENT
  // =========================================================

  protected changerPatient() {
    const nouveau = this.patientIdSaisi().trim();
    if (nouveau && nouveau !== this.patientId()) {
      this.patientId.set(nouveau);
      this.reinitialiserFiltres();
      this.charger();
    }
  }

  protected reinitialiserPatientDefaut() {
    this.patientIdSaisi.set(PATIENT_DEFAUT);
    this.patientId.set(PATIENT_DEFAUT);
    this.reinitialiserFiltres();
    this.charger();
  }

  // =========================================================
  // FILTRES
  // =========================================================

  protected appliquerFiltres() {
    this.filtreMedecinId.set(this.saisieFiltreMedecin().trim());
    this.filtreDu.set(this.saisieFiltredu().trim());
    this.filtreAu.set(this.saisieFiltreAu().trim());
    this.pageCourante.set(0);
    this.chargerConsultations();
  }

  protected reinitialiserFiltres() {
    this.saisieFiltreMedecin.set('');
    this.saisieFiltredu.set('');
    this.saisieFiltreAu.set('');
    this.filtreMedecinId.set('');
    this.filtreDu.set('');
    this.filtreAu.set('');
    this.pageCourante.set(0);
  }

  protected filtresActifs(): boolean {
    return !!(this.filtreMedecinId() || this.filtreDu() || this.filtreAu());
  }

  // =========================================================
  // PAGINATION
  // =========================================================

  protected pagePrecedente() {
    if (this.pageCourante() > 0) {
      this.pageCourante.update(p => p - 1);
      this.chargerConsultations();
    }
  }

  protected pageSuivante() {
    if (this.pageCourante() < this.totalPages() - 1) {
      this.pageCourante.update(p => p + 1);
      this.chargerConsultations();
    }
  }

  protected changerTaillePage(event: Event) {
    const val = parseInt((event.target as HTMLSelectElement).value, 10);
    this.taillePage.set(val);
    this.pageCourante.set(0);
    this.chargerConsultations();
  }

  // =========================================================
  // FORMULAIRE CONSULTATION
  // =========================================================

  protected ouvrirAjoutConsultation() {
    this.enEdition.set(null);
    this.saisie = { medecinId: 'medecin-karim-001', motif: '', diagnostic: '', observations: '', analysesDemandeesStr: '' };
    this.erreurFormulaire.set(null);
    this.formulaireOuvert.set(true);
  }

  protected ouvrirModification(c: Consultation) {
    this.enEdition.set(c);
    this.saisie = {
      medecinId: c.medecinId,
      motif: c.motif,
      diagnostic: c.diagnostic || '',
      observations: c.observations || '',
      analysesDemandeesStr: c.analysesDemandees ? c.analysesDemandees.join(', ') : ''
    };
    this.erreurFormulaire.set(null);
    this.formulaireOuvert.set(true);
  }

  protected fermerFormulaire() {
    if (!this.soumissionEnCours()) {
      this.formulaireOuvert.set(false);
      this.enEdition.set(null);
      this.erreurFormulaire.set(null);
    }
  }

  protected enregistrerConsultation() {
    const d = this.dossier();
    if (!d) return;

    const medecinId = this.saisie.medecinId.trim();
    const motif     = this.saisie.motif.trim();
    const diagnostic = this.saisie.diagnostic.trim();

    if (!medecinId || !motif) {
      this.erreurFormulaire.set('Les champs Médecin et Motif sont obligatoires.');
      return;
    }

    this.soumissionEnCours.set(true);
    this.erreurFormulaire.set(null);

    const edition = this.enEdition();

    if (edition) {
      const payload: ConsultationUpdateRequest = { medecinId, motif, diagnostic, observations: this.saisie.observations.trim() };
      this.api.modifierConsultation(d.id, edition.id, payload).subscribe({
        next: (miseAJour: Consultation) => {
          this.soumissionEnCours.set(false);
          this.formulaireOuvert.set(false);
          this.enEdition.set(null);
          this.afficherToast(`Consultation n° ${miseAJour.id} mise à jour avec succès`);
          this.chargerConsultations();
        },
        error: (err: unknown) => {
          this.soumissionEnCours.set(false);
          const f = this.api.formaterErreur(err);
          this.erreurFormulaire.set(f.detail || f.message);
        }
      });
    } else {
      const analyses = this.saisie.analysesDemandeesStr
        ? this.saisie.analysesDemandeesStr.split(',').map(s => s.trim()).filter(Boolean)
        : [];

      const payload: NouvelleConsultation = { medecinId, motif, diagnostic, observations: this.saisie.observations.trim(), analysesDemandees: analyses };
      this.api.ajouterConsultation(d.id, payload).subscribe({
        next: (creee: Consultation) => {
          this.soumissionEnCours.set(false);
          this.formulaireOuvert.set(false);
          this.afficherToast(`Consultation n° ${creee.id} enregistrée avec succès`);
          this.pageCourante.set(0);
          this.chargerConsultations();
        },
        error: (err: unknown) => {
          this.soumissionEnCours.set(false);
          const f = this.api.formaterErreur(err);
          this.erreurFormulaire.set(f.detail || f.message);
        }
      });
    }
  }

  // =========================================================
  // SUPPRESSION
  // =========================================================

  protected ouvrirSuppression(c: Consultation) {
    this.aSupprimer.set(c);
    this.erreurSuppression.set(null);
  }

  protected fermerSuppression() {
    if (!this.suppressionEnCours()) {
      this.aSupprimer.set(null);
      this.erreurSuppression.set(null);
    }
  }

  protected confirmerSuppression() {
    const c = this.aSupprimer();
    const d = this.dossier();
    if (!c || !d) return;

    this.suppressionEnCours.set(true);
    this.erreurSuppression.set(null);

    this.api.supprimerConsultation(d.id, c.id, c.medecinId).subscribe({
      next: () => {
        this.suppressionEnCours.set(false);
        this.aSupprimer.set(null);
        this.afficherToast(`Consultation n° ${c.id} supprimée avec succès`);
        // Si on est sur la dernière page et qu'elle se vide, reculer d'une page
        if (this.consultationsPage().length === 1 && this.pageCourante() > 0) {
          this.pageCourante.update(p => p - 1);
        }
        this.chargerConsultations();
      },
      error: (err: unknown) => {
        this.suppressionEnCours.set(false);
        const f = this.api.formaterErreur(err);
        this.erreurSuppression.set(f.detail || f.message);
      }
    });
  }

  // =========================================================
  // EXPORT PDF
  // =========================================================

  protected telechargerPdf() {
    this.pdfEnCours.set(true);
    this.erreurPdf.set(null);

    const filtres: FiltresConsultation = {};
    if (this.filtreMedecinId()) filtres.medecinId = this.filtreMedecinId();
    if (this.filtreDu())        filtres.du = this.filtreDu();
    if (this.filtreAu())        filtres.au = this.filtreAu();

    this.api.telechargerPdf(this.patientId(), filtres).subscribe({
      next: (blob: Blob) => {
        this.pdfEnCours.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `consultations-${this.patientId()}.pdf`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
        this.afficherToast('PDF téléchargé avec succès');
      },
      error: (err: unknown) => {
        this.pdfEnCours.set(false);
        const f = this.api.formaterErreur(err);
        this.erreurPdf.set(f.detail || f.message);
      }
    });
  }

  // =========================================================
  // TOAST
  // =========================================================

  protected afficherToast(message: string) {
    this.toast.set(message);
    setTimeout(() => this.toast.set(null), 3500);
  }
}
