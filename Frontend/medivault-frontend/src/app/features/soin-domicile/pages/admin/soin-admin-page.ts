import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { EMPTY, Observable, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import { Icon } from '../../../../shared/icon';
import { CountUp } from '../../../../shared/count-up';
import { SoinDomicileScene } from '../../components/soin-domicile-scene';
import { PanneauLateral } from '../../components/panneau-lateral';
import {
  Infirmier, InfirmierRequest, PHARMACIES, PlanSuivi, RecherchePlans, RechercheVisites, STATUTS, StatistiquesSoin,
  StatutVisite, TYPES_SOIN, Visite, libelleSoin, lienCarte, nomPharmacie
} from '../../models/visite.model';
import { SoinService, messageErreur } from '../../services/soin.service';
import { InfirmierService } from '../../services/infirmier.service';
import { PlanService } from '../../services/plan.service';

type Onglet = 'demandes' | 'infirmiers' | 'plans';
type Suppression = { type: 'demande' | 'infirmier' | 'plan'; id: number; libelle: string };

// L'heure de fin doit être après l'heure de début (même règle que le backend)
const horairesValides = (g: AbstractControl): ValidationErrors | null => {
  const { heureDebut, heureFin } = g.value ?? {};
  return heureDebut && heureFin && heureFin <= heureDebut ? { horaires: true } : null;
};

// ============================================================================================
// BACK-OFFICE (rôle ADMIN) du soin à domicile : vue sur TOUTES les pharmacies.
//  - statistiques (GET /api/visites/statistiques, requêtes GROUP BY)
//  - toutes les demandes avec recherche multicritère, annulation et suppression
//  - CRUD des infirmiers de toutes les pharmacies (Reactive Form avec validation)
//  - tous les plans de suivi, recherche et arrêt
// ============================================================================================
@Component({
  selector: 'mv-soin-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ReactiveFormsModule, DatePipe, Icon, CountUp, SoinDomicileScene, PanneauLateral],
  templateUrl: './soin-admin-page.html'
})
export class SoinAdminPage implements OnInit {
  private soin = inject(SoinService);
  private infirmierApi = inject(InfirmierService);
  private planApi = inject(PlanService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  protected readonly pharmacies = PHARMACIES;
  protected readonly typesSoin = TYPES_SOIN;
  protected readonly statuts = STATUTS;
  protected readonly listeStatuts = Object.entries(STATUTS).map(([valeur, s]) => ({ valeur: valeur as StatutVisite, libelle: s.libelle }));
  protected readonly libelleSoin = libelleSoin;
  protected readonly nomPharmacie = nomPharmacie;
  protected readonly lienCarte = lienCarte;

  protected readonly onglet = signal<Onglet>('demandes');
  protected readonly toast = signal<{ texte: string; ok: boolean } | null>(null);
  protected readonly stats = signal<StatistiquesSoin | null>(null);

  // ---------- Demandes ----------
  protected filtre: RechercheVisites = this.filtreVide();
  protected readonly demandes = signal<Visite[]>([]);
  protected readonly detail = signal<Visite | null>(null);
  private readonly rechercheDemandes$ = new Subject<RechercheVisites>();

  // ---------- Infirmiers ----------
  protected rechercheInf = '';
  protected pharmacieInf = '';
  protected readonly infirmiers = signal<Infirmier[]>([]);
  protected readonly panneauInf = signal(false);
  protected readonly enEdition = signal<number | null>(null);
  protected readonly envoi = signal(false);
  protected readonly erreurForm = signal('');
  private readonly rechercheInfirmiers$ = new Subject<{ pharmacieId: string; q: string }>();
  protected readonly formInf = this.fb.nonNullable.group({
    prenom: ['', [Validators.required, Validators.maxLength(60)]],
    nom: ['', [Validators.required, Validators.maxLength(60)]],
    telephone: ['', Validators.pattern(/^$|^[+0-9 ]{8,20}$/)],
    heureDebut: ['08:00', Validators.required],
    heureFin: ['16:00', Validators.required],
    pharmacieId: ['', Validators.required]
  }, { validators: horairesValides });

  // ---------- Plans ----------
  protected filtrePlans: RecherchePlans = { q: '', typeSoin: '', enCours: '' };
  protected readonly plans = signal<PlanSuivi[]>([]);
  private readonly recherchePlans$ = new Subject<RecherchePlans>();

  // ---------- Suppression (fenêtre de confirmation) ----------
  protected readonly aSupprimer = signal<Suppression | null>(null);

  // ---------- Graphiques en barres ----------
  protected readonly barresStatut = computed(() =>
    this.barres(this.stats()?.parStatut, k => STATUTS[k as StatutVisite]?.libelle ?? k));
  protected readonly barresType = computed(() => this.barres(this.stats()?.parType, k => libelleSoin(k as never)));
  protected readonly lignesPharmacies = computed(() => {
    const s = this.stats();
    return PHARMACIES.map(p => ({
      p, demandes: s?.demandesParPharmacie[p.id] ?? 0, infirmiers: s?.infirmiersParPharmacie[p.id] ?? 0
    }));
  });
  protected readonly topInfirmiers = computed(() => this.barres(this.stats()?.topInfirmiers, k => k));

  ngOnInit(): void {
    // Chaque recherche attend 300 ms après la dernière frappe ; seule la dernière réponse compte
    this.rechercheDemandes$.pipe(
      debounceTime(300),
      switchMap(f => this.sansErreur(this.soin.rechercher(f))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(v => this.demandes.set([...v].reverse()));          // les plus récentes en premier

    this.rechercheInfirmiers$.pipe(
      debounceTime(300),
      switchMap(c => this.sansErreur(this.infirmierApi.rechercher(c.pharmacieId, c.q))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(l => this.infirmiers.set(l));

    this.recherchePlans$.pipe(
      debounceTime(300),
      switchMap(f => this.sansErreur(this.planApi.rechercher(f))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(p => this.plans.set(p));

    this.toutCharger();
  }

  protected toutCharger(): void {
    this.soin.statistiques().subscribe({ next: s => this.stats.set(s), error: e => this.notifier(e) });
    this.rechercherDemandes();
    this.rechercherInfirmiers();
    this.rechercherPlans();
  }

  // ======================= Demandes =======================
  protected rechercherDemandes(): void { this.rechercheDemandes$.next({ ...this.filtre }); }

  protected filtrerStatut(statut: string): void {
    this.filtre = { ...this.filtre, statut: this.filtre.statut === statut ? '' : statut as StatutVisite };
    this.onglet.set('demandes');
    this.rechercherDemandes();
  }

  protected filtrerPharmacie(id: string): void {
    this.filtre = { ...this.filtre, pharmacieId: this.filtre.pharmacieId === id ? '' : id };
    this.onglet.set('demandes');
    this.rechercherDemandes();
  }

  protected effacerFiltres(): void {
    this.filtre = this.filtreVide();
    this.rechercherDemandes();
  }

  protected readonly filtresActifs = () => Object.values(this.filtre).some(v => !!v);

  protected annuler(v: Visite): void {
    this.soin.annuler(v.id).subscribe({
      next: () => { this.notifier(`Demande n° ${v.id} annulée`); this.detail.set(null); this.toutCharger(); },
      error: e => this.notifier(e)
    });
  }

  // ======================= Infirmiers (CRUD) =======================
  protected rechercherInfirmiers(): void {
    this.rechercheInfirmiers$.next({ pharmacieId: this.pharmacieInf, q: this.rechercheInf.trim() });
  }

  protected ouvrirAjout(): void {
    this.enEdition.set(null);
    this.formInf.reset({ prenom: '', nom: '', telephone: '', heureDebut: '08:00', heureFin: '16:00', pharmacieId: this.pharmacieInf });
    this.erreurForm.set('');
    this.panneauInf.set(true);
  }

  protected ouvrirEdition(i: Infirmier): void {
    this.enEdition.set(i.id);
    this.formInf.reset({
      prenom: i.prenom, nom: i.nom, telephone: i.telephone ?? '',
      heureDebut: i.heureDebut.slice(0, 5), heureFin: i.heureFin.slice(0, 5), pharmacieId: i.pharmacieId
    });
    this.erreurForm.set('');
    this.panneauInf.set(true);
  }

  protected enregistrerInfirmier(): void {
    if (this.formInf.invalid) { this.formInf.markAllAsTouched(); return; }
    const r: InfirmierRequest = this.formInf.getRawValue();
    const id = this.enEdition();
    this.envoi.set(true);
    (id === null ? this.infirmierApi.ajouter(r) : this.infirmierApi.modifier(id, r)).subscribe({
      next: i => {
        this.envoi.set(false);
        this.panneauInf.set(false);
        this.notifier(id === null ? `${i.nomComplet} ajouté(e) à ${nomPharmacie(i.pharmacieId)}` : `${i.nomComplet} modifié(e)`);
        this.toutCharger();
      },
      error: (e: HttpErrorResponse) => { this.envoi.set(false); this.erreurForm.set(messageErreur(e)); }
    });
  }

  protected invalide(champ: string): boolean {
    const c = this.formInf.get(champ);
    return !!c && c.invalid && c.touched;
  }

  // ======================= Plans =======================
  protected rechercherPlans(): void { this.recherchePlans$.next({ ...this.filtrePlans }); }

  protected tonStatut(cle: string): string { return STATUTS[cle as StatutVisite]?.ton ?? ''; }

  protected enCours(p: PlanSuivi): boolean { return p.dateFin >= new Date().toISOString().slice(0, 10); }

  // ======================= Suppression =======================
  protected demanderSuppression(type: Suppression['type'], id: number, libelle: string): void {
    this.aSupprimer.set({ type, id, libelle });
  }

  protected confirmerSuppression(): void {
    const x = this.aSupprimer();
    if (!x) return;
    const appel = x.type === 'demande' ? this.soin.supprimer(x.id)
      : x.type === 'infirmier' ? this.infirmierApi.supprimer(x.id)
      : this.planApi.arreter(x.id);
    appel.subscribe({
      next: () => {
        this.aSupprimer.set(null);
        this.detail.set(null);
        this.notifier(x.type === 'plan' ? `Plan de ${x.libelle} arrêté` : `« ${x.libelle} » supprimé`);
        this.toutCharger();
      },
      error: e => { this.aSupprimer.set(null); this.notifier(e); }
    });
  }

  // ======================= Outils =======================
  private barres(m: Record<string, number> | undefined, libelle: (k: string) => string) {
    const lignes = Object.entries(m ?? {}).map(([cle, nb]) => ({ cle, libelle: libelle(cle), nb }));
    const max = Math.max(1, ...lignes.map(l => l.nb));
    return lignes.sort((a, b) => b.nb - a.nb).map(l => ({ ...l, pct: Math.round((l.nb / max) * 100) }));
  }

  private sansErreur<T>(o: Observable<T>): Observable<T> {
    return o.pipe(catchError(e => { this.notifier(e); return EMPTY; }));
  }

  private notifier(m: string | HttpErrorResponse): void {
    const ok = typeof m === 'string';
    this.toast.set({ texte: ok ? m : messageErreur(m), ok });
    setTimeout(() => this.toast.set(null), 3500);
  }

  private filtreVide(): RechercheVisites { return { q: '', pharmacieId: '', statut: '', typeSoin: '', du: '', au: '' }; }
}
