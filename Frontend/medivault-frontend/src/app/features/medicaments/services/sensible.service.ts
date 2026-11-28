import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, shareReplay, tap, timeout } from 'rxjs';
import { AuthService } from '../../../core/auth.service';
import {
  Attribution, Bilan, Disponibilite, LotRequest, MedicamentSensible, NouveauMedicament, Pharmacie,
  ResultatLot, ResultatVente, StatutAttribution, VenteSensible, EtapeVente, moisDe,
  PrevisionMedicament, RisquePharmacie, TentativeRefusee
} from '../models/sensible.model';

// ---------------------------------------------------------------------------
// Service du medicament-service (via la Gateway : /api/**).
// Toutes les données viennent du back : aucune donnée fictive dans le front.
// Si le back ne répond pas, les listes restent vides et enLigne passe à false (les pages l'affichent).
// ---------------------------------------------------------------------------

/**
 * Identifiants des pharmacies côté back : « Pharmacie Centrale » est le compte de démonstration du user-service
 * (id texte « pharmacie-centrale »). Les autres gardent leur numéro. Le front, lui, compare des nombres.
 */
const ID_BACK: Record<number, string> = {};   // rempli avec la vraie liste de pharmacies du user-service dès qu'elle est chargée
const versBack = (id: number): string => ID_BACK[id] ?? String(id);
const depuisBack = (id: unknown): unknown => {
  if (typeof id !== 'string') return id;
  const connu = Object.entries(ID_BACK).find(([, texte]) => texte === id);
  return connu ? Number(connu[0]) : /^\d+$/.test(id) ? Number(id) : id;
};

/** Un compte « pharmacien » tel que le renvoie le user-service */
interface ComptePharmacie { id: string; nom: string; prenom: string; nomEtablissement?: string; ville?: string; }

/** Les pharmacies viennent du user-service (comptes PHARMACIEN) : la liste se remplit au chargement */
export const PHARMACIES: Pharmacie[] = [];

const moisCourant = moisDe(new Date());

@Injectable({ providedIn: 'root' })
export class SensibleService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  /** true : le back répond · false : le back ne répond pas · null : pas encore testé */
  readonly enLigne = signal<boolean | null>(null);
  readonly moisCourant = moisCourant;
  /** La pharmacie connectée (viendra du jeton Keycloak) */
  readonly pharmacieId = signal(1);
  readonly pharmacies = PHARMACIES;

  readonly medicaments = signal<MedicamentSensible[]>([]);
  readonly attributions = signal<Attribution[]>([]);
  readonly ventes = signal<VenteSensible[]>([]);
  // surveillance (venue du back)
  readonly risques = signal<RisquePharmacie[]>([]);
  readonly refus = signal<TentativeRefusee[]>([]);
  readonly previsions = signal<PrevisionMedicament[]>([]);

  private pret$?: Observable<void>;

  /** Le back identifie les pharmacies par un texte ("pharmacie-centrale", "2"…) ; le front les compare comme des nombres */
  private deApi<T extends { pharmacieId: unknown }>(x: T): T {
    return { ...x, pharmacieId: depuisBack(x.pharmacieId) };
  }
  private rafraichirQuotas() {
    this.http.get<Attribution[]>('/api/attributions').pipe(catchError(() => of(null))).subscribe(a => a && this.attributions.set(a.map(x => this.deApi(x))));
    this.rafraichirSurveillance();
  }

  /** Score de risque, historique des refus et prévisions : chacun se recharge seul, et reste vide s'il ne répond pas */
  private rafraichirSurveillance() {
    this.http.get<RisquePharmacie[]>('/api/risques').pipe(catchError(() => of(null))).subscribe(r => r && this.risques.set(r.map(x => this.deApi(x))));
    this.http.get<TentativeRefusee[]>('/api/tentatives-refusees').pipe(catchError(() => of(null))).subscribe(t => t && this.refus.set(t.map(x => this.deApi(x))));
    this.http.get<PrevisionMedicament[]>('/api/previsions/medicaments').pipe(catchError(() => of(null))).subscribe(p => p && this.previsions.set(p));
  }

  /** La prévision d'épuisement de ce médicament ce mois-ci (s'il a des quotas) */
  prevision(medicamentId: number) { return this.previsions().find(p => p.medicamentId === medicamentId); }

  // ---------- Chiffres utiles aux pages ----------
  readonly attributionsDuMois = computed(() => this.attributions().filter(a => a.mois === this.moisCourant));
  readonly totalAttribue = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.quantiteAttribuee, 0));
  readonly totalVendu = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.quantiteVendue, 0));
  readonly avertissements = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.avertissements, 0));
  readonly pharmaciesSuspendues = computed(() => new Set(this.attributionsDuMois().filter(a => a.statut === 'SUSPENDUE').map(a => a.pharmacieId)).size);

  medicament(id: number) { return this.medicaments().find(m => m.id === id); }
  pharmacie(id: number) { return this.pharmacies.find(p => p.id === id); }
  /** Le code de l'ordonnance vient de la vente (renvoyé par le back) */
  codeOrdonnance(id: number) { return this.ventes().find(v => v.ordonnanceId === id)?.ordonnanceCode ?? `ORD-${id}`; }
  statutPharmacie(id: number) {
    return this.attributionsDuMois().some(a => a.pharmacieId === id && a.statut === 'SUSPENDUE') ? 'SUSPENDUE' : 'ACTIVE';
  }

  /** Charge les données une seule fois depuis le back */
  charger(): Observable<void> {
    return (this.pret$ ??= forkJoin({
      m: this.http.get<MedicamentSensible[]>('/api/medicaments-sensibles'),
      a: this.http.get<Attribution[]>('/api/attributions'),
      v: this.http.get<VenteSensible[]>('/api/ventes'),
      // les vraies pharmacies viennent du user-service ; s'il ne répond pas, la liste reste vide (les pages affichent l'identifiant)
      p: this.http.get<ComptePharmacie[]>('/api/professionnels?type=PHARMACIEN').pipe(catchError(() => of([] as ComptePharmacie[])))
    }).pipe(
      timeout(3000),
      tap(({ m, a, v, p }) => {
        this.appliquerPharmacies(p);
        this.medicaments.set(m);
        this.attributions.set(a.map(x => this.deApi(x)));
        this.ventes.set(v.map(x => this.deApi(x)));
        this.enLigne.set(true);
        this.rafraichirSurveillance();
      }),
      map(() => void 0),
      catchError(() => {
        this.enLigne.set(false);
        return of(void 0);
      }),
      shareReplay(1)
    ));
  }

  /** Remplit la liste avec les vraies pharmacies (numéro 1, 2, 3… pour le front, id du user-service pour le back) */
  private appliquerPharmacies(liste: ComptePharmacie[]) {
    if (!liste.length) return;
    // ordre stable : la Pharmacie Centrale d'abord, puis par nom (l'API ne garantit aucun ordre)
    const comptes = [...liste].sort((a, b) => (a.id === 'pharmacie-centrale' ? -1 : b.id === 'pharmacie-centrale' ? 1
      : (a.nomEtablissement ?? '').localeCompare(b.nomEtablissement ?? '')));
    Object.keys(ID_BACK).forEach(k => delete ID_BACK[Number(k)]);
    PHARMACIES.splice(0, PHARMACIES.length, ...comptes.map((c, i) => ({ id: i + 1, nom: c.nomEtablissement ?? `${c.prenom} ${c.nom}`, ville: c.ville ?? '' })));
    comptes.forEach((c, i) => (ID_BACK[i + 1] = c.id));
    // un pharmacien connecté (vrai compte Keycloak) voit SA pharmacie : l'identifiant du compte est celui de la pharmacie
    const moi = this.auth.utilisateur();
    if (moi?.role === 'PHARMACIEN' && moi.id) {
      const n = comptes.findIndex(c => c.id === moi.id);
      if (n >= 0) this.pharmacieId.set(n + 1);
    }
  }

  // ---------- Médicaments sensibles (CRUD) ----------
  creerMedicament(m: NouveauMedicament): Observable<MedicamentSensible> {
    return this.http.post<MedicamentSensible>('/api/medicaments-sensibles', m).pipe(tap(c => this.medicaments.update(l => [...l, c])));
  }

  modifierMedicament(id: number, m: NouveauMedicament): Observable<MedicamentSensible> {
    return this.http.put<MedicamentSensible>(`/api/medicaments-sensibles/${id}`, m).pipe(tap(c => this.medicaments.update(l => l.map(x => (x.id === id ? c : x)))));
  }

  supprimerMedicament(id: number): Observable<void> {
    return this.http.delete<void>(`/api/medicaments-sensibles/${id}`).pipe(tap(() => {
      this.medicaments.update(l => l.filter(x => x.id !== id));
      this.attributions.update(l => l.filter(a => a.medicamentId !== id));
    }));
  }

  // ---------- Quotas (attributions) ----------
  attribuerLot(req: LotRequest): Observable<ResultatLot> {
    const corps = { medicamentId: req.medicamentId, mois: req.mois, quantiteAttribuee: req.quantite, pharmacieIds: req.pharmacieIds.map(versBack) };
    return this.http.post<{ attributions: Attribution[]; pharmaciesIgnorees: string[] }>('/api/attributions/lot', corps).pipe(
      map(r => ({ creees: r.attributions.map(x => this.deApi(x)), ignorees: r.pharmaciesIgnorees.map(i => depuisBack(i) as number) } as ResultatLot)),
      tap(r => this.attributions.update(l => [...l, ...r.creees]))
    );
  }

  modifierAttribution(id: number, quantiteAttribuee: number): Observable<Attribution> {
    const actuelle = this.attributions().find(a => a.id === id)!;
    return this.http.put<Attribution>(`/api/attributions/${id}`, { quantiteAttribuee, statut: actuelle.statut }).pipe(
      map(a => this.deApi(a)),
      tap(a => this.attributions.update(l => l.map(x => (x.id === id ? a : x))))
    );
  }

  supprimerAttribution(id: number): Observable<void> {
    return this.http.delete<void>(`/api/attributions/${id}`).pipe(tap(() => this.attributions.update(l => l.filter(a => a.id !== id))));
  }

  bilan(id: number): Observable<Bilan> {
    return this.http.get<{ recu: number; vendu: number; reste: number; avertissements: number; statut: StatutAttribution }>(`/api/attributions/${id}/bilan`).pipe(
      map(b => ({ attribuee: b.recu, vendue: b.vendu, restant: b.reste, taux: b.recu ? Math.round((b.vendu / b.recu) * 100) : 0, avertissements: b.avertissements, statut: b.statut } as Bilan))
    );
  }

  // ---------- Disponibilités (pour la consultation) ----------
  disponibilites(medicamentId: number): Observable<Disponibilite[]> {
    return this.http.get<{ pharmacieId: string; reste: number; etat: Disponibilite['etat'] }[]>('/api/disponibilites', { params: new HttpParams().set('medicamentId', medicamentId) }).pipe(
      map(liste => liste.map(d => {
        const id = this.deApi(d).pharmacieId as unknown as number;
        const p = this.pharmacie(id);
        return { pharmacieId: id, nom: p?.nom ?? `Pharmacie ${d.pharmacieId}`, ville: p?.ville ?? '', restant: d.reste, etat: d.etat } as Disponibilite;
      })),
      catchError(() => of([] as Disponibilite[]))
    );
  }

  // ---------- Déclarer une vente (RG1, RG2, RG3) : les règles sont appliquées par le back ----------
  declarerVente(req: { ordonnanceCode: string; medicamentId: number; quantite: number }): Observable<ResultatVente> {
    const pid = this.pharmacieId();
    const etapes = (): EtapeVente[] => [
      { libelle: 'Pharmacie active', ok: null },
      { libelle: 'Ordonnance validée contenant ce médicament', ok: null },
      { libelle: 'Quota du mois suffisant', ok: null }
    ];

    return this.http.post<VenteSensible>('/api/ventes', { pharmacieId: versBack(pid), ...req }).pipe(
      map(v => this.deApi(v)),
      map(v => {
        this.ventes.update(l => [v, ...l]);
        this.attributions.update(l => l.map(a => (a.pharmacieId === pid && a.medicamentId === req.medicamentId && a.mois === this.moisCourant ? { ...a, quantiteVendue: a.quantiteVendue + req.quantite } : a)));
        const e = etapes().map(x => ({ ...x, ok: true }));
        return { ok: true, code: 201, message: 'Vente enregistrée.', etapes: e, avertissement: false, suspendue: false, vente: v } as ResultatVente;
      }),
      catchError((err: HttpErrorResponse) => {
        const e = etapes();
        const detail = err.error?.detail as string | undefined;
        const suspendue = !!err.error?.suspendue;
        // le back a pu enregistrer un avertissement : on relit les quotas pour que l'écran reste exact
        if (err.status === 403 || err.status === 409) this.rafraichirQuotas();
        if (err.status === 403) { e[0].ok = false; return of({ ok: false, code: 403, message: detail ?? 'Votre pharmacie est inactive : aucune vente possible.', etapes: e, avertissement: false, suspendue } as ResultatVente); }
        if (err.status === 409) {
          e[0].ok = true;
          const quota = err.error?.title === 'Quota épuisé';
          e[1].ok = quota; e[2].ok = quota ? false : null; if (!quota) e[1].ok = false;
          return of({ ok: false, code: 409, message: detail ?? 'Ordonnance invalide ou quota épuisé.', etapes: e, avertissement: true, suspendue } as ResultatVente);
        }
        return of({ ok: false, code: 503, message: 'Un service est momentanément indisponible. Réessayez dans un instant.', etapes: e, avertissement: false, suspendue: false } as ResultatVente);
      })
    );
  }
}
