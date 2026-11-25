import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { PlanService } from '../services/plan.service';
import { messageErreur } from '../services/soin.service';
import {
  FREQUENCES, PHARMACIES, PlanSuivi, PlanSuiviRequest, RecherchePlans, STATUTS, TYPES_SOIN, Visite, estAnormale, libelleSoin,
  nomPharmacie
} from '../models/visite.model';

// Espace médecin : prescrire un suivi à domicile régulier et consulter les mesures relevées
@Component({
  selector: 'mv-espace-medecin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, Icon],
  templateUrl: './espace-medecin.html'
})
export class EspaceMedecin implements OnInit {
  private plans = inject(PlanService);
  private auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  protected readonly typesSoin = TYPES_SOIN;
  protected readonly frequences = FREQUENCES;
  protected readonly pharmacies = PHARMACIES;
  protected readonly statuts = STATUTS;
  protected readonly libelleSoin = libelleSoin;
  protected readonly nomPharmacie = nomPharmacie;
  protected readonly estAnormale = estAnormale;

  protected readonly tousMesPlans = signal<PlanSuivi[]>([]);

  // ---------- Recherche des plans (/api/plans/recherche?medecinId=...) ----------
  protected filtre: RecherchePlans = this.filtreVide();
  protected readonly resultats = signal<PlanSuivi[] | null>(null);   // null = pas de recherche
  private readonly recherche$ = new Subject<RecherchePlans>();
  protected readonly mesPlans = computed(() => this.resultats() ?? this.tousMesPlans());
  protected readonly ouvert = signal<number | null>(null);
  protected readonly visites = signal<Visite[]>([]);
  protected readonly message = signal('');
  protected readonly erreur = signal('');

  protected plan = this.planVide();
  protected seuilTension = '14/9';
  protected seuilGlycemie: number | null = 1.8;

  private get medecinId(): string { return this.auth.utilisateur()?.email ?? ''; }

  ngOnInit(): void {
    this.recherche$.pipe(
      debounceTime(300),
      switchMap(f => this.plans.rechercher(f).pipe(catchError(e => { this.afficherErreur(e); return EMPTY; }))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(p => this.resultats.set(p));
    this.charger();
  }

  protected charger(): void {
    this.plans.lister(this.medecinId).subscribe({
      next: p => this.tousMesPlans.set([...p].reverse()),
      error: e => this.afficherErreur(e)
    });
    this.rechercher();
  }

  /** Recherche parmi MES plans : le medecinId est toujours envoyé. */
  protected rechercher(): void {
    const { q, typeSoin, enCours } = this.filtre;
    if (!q?.trim() && !typeSoin && !enCours) { this.resultats.set(null); return; }
    this.recherche$.next({ ...this.filtre, medecinId: this.medecinId });
  }

  protected effacerRecherche(): void {
    this.filtre = this.filtreVide();
    this.resultats.set(null);
  }

  private filtreVide(): RecherchePlans { return { q: '', typeSoin: '', enCours: '' }; }

  protected creer(): void {
    const seuils = [
      this.seuilTension ? 'tension>' + this.seuilTension : '',
      this.seuilGlycemie ? 'glycemie>' + this.seuilGlycemie : ''
    ].filter(Boolean).join(';');
    const r: PlanSuiviRequest = { ...this.plan, medecinId: this.medecinId, seuilAlerte: seuils || undefined };
    this.plans.creer(r).subscribe({
      next: p => {
        this.succes(`Suivi prescrit pour ${p.patientId} : les visites sont envoyées à ${nomPharmacie(p.pharmacieId)}.`);
        this.plan = this.planVide();
        this.charger();
        this.voirVisites(p);
      },
      error: e => this.afficherErreur(e)
    });
  }

  protected voirVisites(p: PlanSuivi): void {
    if (this.ouvert() === p.id) { this.ouvert.set(null); return; }
    this.plans.visites(p.id).subscribe({
      next: v => { this.visites.set(v); this.ouvert.set(p.id); },
      error: e => this.afficherErreur(e)
    });
  }

  protected arreter(p: PlanSuivi): void {
    this.plans.arreter(p.id).subscribe({
      next: () => { this.succes('Suivi arrêté : les visites à venir sont annulées.'); this.ouvert.set(null); this.charger(); },
      error: e => this.afficherErreur(e)
    });
  }

  protected enCours(p: PlanSuivi): boolean {
    return p.dateFin >= new Date().toISOString().slice(0, 10);
  }

  protected nbAlertes(p: PlanSuivi): number {
    return this.ouvert() === p.id ? this.visites().filter(v => estAnormale(v, p.seuilAlerte)).length : 0;
  }

  protected libelleFrequence(f: string): string {
    return this.frequences.find(x => x.valeur === f)?.libelle ?? f;
  }

  private planVide(): PlanSuiviRequest {
    return {
      patientId: '', medecinId: '', pharmacieId: PHARMACIES[0].id, adresse: '', typeSoin: 'GLYCEMIE',
      frequence: 'HEBDOMADAIRE', dateDebut: '', dateFin: '', heureVisite: '10:00'
    };
  }

  private succes(m: string): void { this.erreur.set(''); this.message.set(m); }
  private afficherErreur(e: HttpErrorResponse): void { this.message.set(''); this.erreur.set(messageErreur(e)); }
}
