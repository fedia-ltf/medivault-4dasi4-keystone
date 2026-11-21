import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, shareReplay, tap, throwError, timeout } from 'rxjs';
import {
  Attribution, Bilan, Disponibilite, LotRequest, MedicamentSensible, NouveauMedicament, Pharmacie,
  ResultatLot, ResultatVente, StatutAttribution, VenteSensible, EtapeVente, moisDe
} from '../models/sensible.model';

// ---------------------------------------------------------------------------
// Service du medicament-service (via la Gateway : /api/**).
// Si l'API ne répond pas, il passe en mode démonstration : mêmes règles métier
// (RG1 ordonnance validée, RG2 quota, RG3 avertissements → suspension), en mémoire.
// ---------------------------------------------------------------------------

/**
 * Identifiants des pharmacies côté back : « Pharmacie Centrale » est le compte de démonstration du user-service
 * (id texte « pharmacie-centrale »). Les autres gardent leur numéro. Le front, lui, compare des nombres.
 */
const ID_BACK: Record<number, string> = { 1: 'pharmacie-centrale' };
const versBack = (id: number): string => ID_BACK[id] ?? String(id);
const depuisBack = (id: unknown): unknown => {
  if (typeof id !== 'string') return id;
  const connu = Object.entries(ID_BACK).find(([, texte]) => texte === id);
  return connu ? Number(connu[0]) : /^\d+$/.test(id) ? Number(id) : id;
};

export const PHARMACIES: Pharmacie[] = [
  { id: 1, nom: 'Pharmacie Centrale', ville: 'Tunis' },
  { id: 2, nom: 'Pharmacie de la Marsa', ville: 'La Marsa' },
  { id: 3, nom: 'Pharmacie El Manar', ville: 'Tunis' },
  { id: 4, nom: 'Pharmacie Ibn Sina', ville: 'Sfax' },
  { id: 5, nom: 'Pharmacie Les Jasmins', ville: 'Sousse' },
  { id: 6, nom: 'Pharmacie Carthage', ville: 'Carthage' },
  { id: 7, nom: 'Pharmacie El Menzah', ville: 'Tunis' },
  { id: 8, nom: 'Pharmacie Bizerte Centre', ville: 'Bizerte' }
];

const MEDICAMENTS: MedicamentSensible[] = [
  { id: 1, nomCommercial: 'Subutex 8 mg', dci: 'Buprénorphine', categorie: 'STUPEFIANT', description: 'Traitement de substitution des dépendances aux opioïdes : réduit le besoin et les symptômes de manque.', precautions: 'Ne pas associer à l\'alcool ni aux benzodiazépines. Comprimé à laisser fondre sous la langue. Risque de somnolence.' },
  { id: 2, nomCommercial: 'Méthadone 40 mg', dci: 'Méthadone', categorie: 'STUPEFIANT', description: 'Traitement de substitution aux opioïdes et antalgique de palier 3.', precautions: 'Dose strictement individuelle. Risque de surdosage en début de traitement. Éviter la conduite.' },
  { id: 3, nomCommercial: 'Skenan 30 mg', dci: 'Morphine', categorie: 'STUPEFIANT', description: 'Antalgique opioïde fort pour les douleurs intenses, notamment cancéreuses.', precautions: 'Constipation et somnolence fréquentes. Ne jamais écraser la gélule. Risque de dépendance.' },
  { id: 4, nomCommercial: 'Ritaline 10 mg', dci: 'Méthylphénidate', categorie: 'PSYCHOTROPE', description: 'Stimulant du système nerveux central utilisé dans le trouble du déficit de l\'attention.', precautions: 'À prendre le matin. Peut perturber le sommeil et l\'appétit. Surveillance de la tension.' },
  { id: 5, nomCommercial: 'Rivotril 2 mg', dci: 'Clonazépam', categorie: 'PSYCHOTROPE', description: 'Antiépileptique et anxiolytique de la famille des benzodiazépines.', precautions: 'Traitement court. Risque de dépendance et de somnolence. Ne pas arrêter brutalement.' },
  { id: 6, nomCommercial: 'Xanax 0,5 mg', dci: 'Alprazolam', categorie: 'PSYCHOTROPE', description: 'Anxiolytique à action rapide, pour les états d\'anxiété sévères.', precautions: 'Éviter l\'alcool. Ne pas conduire. Durée de traitement la plus courte possible.' },
  { id: 7, nomCommercial: 'Misoprostol 200 µg', dci: 'Misoprostol', categorie: 'ABORTIF', description: 'Provoque des contractions de l\'utérus : utilisé sous contrôle médical pour interrompre une grossesse.', precautions: 'Strictement contre-indiqué en dehors d\'une prescription encadrée. Saignements et douleurs à surveiller.' }
];

const ORDONNANCES = [
  { id: 101, code: 'ORD-A7F3', validee: true, medicaments: [1, 2], medecin: 'Dr Karim Benali', patient: 'Ahmed Trabelsi', patientId: 201 },
  { id: 102, code: 'ORD-C884', validee: true, medicaments: [4, 5], medecin: 'Dr Sonia Gharbi', patient: 'Youssef Ben Ali', patientId: 202 },
  { id: 103, code: 'ORD-D102', validee: false, medicaments: [1], medecin: 'Dr Karim Benali', patient: 'Leila Haddad', patientId: 203 },
  { id: 104, code: 'ORD-B219', validee: true, medicaments: [3, 6], medecin: 'Dr Nabil Cherif', patient: 'Sarra Mejri', patientId: 204 },
  { id: 105, code: 'ORD-E410', validee: true, medicaments: [7, 1], medecin: 'Dr Sonia Gharbi', patient: 'Amira Jlassi', patientId: 205 }
];

const moisCourant = moisDe(new Date());

function attributionsDemo(): Attribution[] {
  const ratios = [0.42, 0.68, 0.93, 0.55, 0.97, 0.3, 0.8, 0.62];
  const liste: Attribution[] = [];
  PHARMACIES.forEach(p => {
    [(p.id % 7) + 1, ((p.id + 2) % 7) + 1, ((p.id + 4) % 7) + 1].forEach((medicamentId, k) => {
      const attribuee = 40 + ((p.id * 13 + medicamentId * 7) % 5) * 20;
      let avertissements = 0;
      if (p.id === 1 && k === 1) avertissements = 1;
      if (p.id === 3 && k === 0) avertissements = 1;
      if (p.id === 6 && k === 0) avertissements = 2;
      liste.push({
        id: liste.length + 1, pharmacieId: p.id, medicamentId, mois: moisCourant, quantiteAttribuee: attribuee,
        quantiteVendue: Math.round(attribuee * ratios[(p.id + medicamentId) % ratios.length]),
        avertissements, statut: avertissements >= 2 ? 'SUSPENDUE' : 'ACTIVE'
      });
    });
  });
  return liste;
}

function ventesDemo(attributions: Attribution[]): VenteSensible[] {
  const ventes: VenteSensible[] = [];
  const jour = Math.max(1, new Date().getDate());
  attributions.forEach(a => {
    let reste = a.quantiteVendue, i = 0;
    while (reste > 0) {
      const q = Math.min(reste, 4 + ((a.id + i) % 9));
      const valides = ORDONNANCES.filter(o => o.validee && o.medicaments.includes(a.medicamentId));
      const o = (valides.length ? valides : ORDONNANCES.slice(0, 1))[(a.id + i) % Math.max(1, valides.length)];
      ventes.push({
        id: ventes.length + 1, patientId: o.patientId, ordonnanceId: o.id, quantite: q,
        date: `${moisCourant}-${String(1 + ((a.id * 3 + i * 5) % jour)).padStart(2, '0')}`,
        pharmacieId: a.pharmacieId, medicamentId: a.medicamentId
      });
      reste -= q;
      i++;
    }
  });
  return ventes.sort((x, y) => y.date.localeCompare(x.date));
}

@Injectable({ providedIn: 'root' })
export class SensibleService {
  private http = inject(HttpClient);

  /** true : l'API répond · false : mode démonstration · null : pas encore testé */
  readonly enLigne = signal<boolean | null>(null);
  readonly moisCourant = moisCourant;
  /** La pharmacie connectée (viendra du jeton Keycloak) */
  readonly pharmacieId = signal(1);
  readonly pharmacies = PHARMACIES;

  readonly medicaments = signal<MedicamentSensible[]>(MEDICAMENTS);
  readonly attributions = signal<Attribution[]>(attributionsDemo());
  readonly ventes = signal<VenteSensible[]>(ventesDemo(this.attributions()));

  private pret$?: Observable<void>;

  /** Le back identifie les pharmacies par un texte ("pharmacie-centrale", "2"…) ; le front les compare comme des nombres */
  private deApi<T extends { pharmacieId: unknown }>(x: T): T {
    return { ...x, pharmacieId: depuisBack(x.pharmacieId) };
  }
  private rafraichirQuotas() {
    this.http.get<Attribution[]>('/api/attributions').pipe(catchError(() => of(null))).subscribe(a => a && this.attributions.set(a.map(x => this.deApi(x))));
  }

  // ---------- Chiffres utiles aux pages ----------
  readonly attributionsDuMois = computed(() => this.attributions().filter(a => a.mois === this.moisCourant));
  readonly totalAttribue = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.quantiteAttribuee, 0));
  readonly totalVendu = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.quantiteVendue, 0));
  readonly avertissements = computed(() => this.attributionsDuMois().reduce((t, a) => t + a.avertissements, 0));
  readonly pharmaciesSuspendues = computed(() => new Set(this.attributionsDuMois().filter(a => a.statut === 'SUSPENDUE').map(a => a.pharmacieId)).size);

  medicament(id: number) { return this.medicaments().find(m => m.id === id); }
  pharmacie(id: number) { return this.pharmacies.find(p => p.id === id); }
  ordonnance(id: number) { return ORDONNANCES.find(o => o.id === id); }
  codeOrdonnance(id: number) { return this.ordonnance(id)?.code ?? `ORD-${id}`; }
  statutPharmacie(id: number) {
    return this.attributionsDuMois().some(a => a.pharmacieId === id && a.statut === 'SUSPENDUE') ? 'SUSPENDUE' : 'ACTIVE';
  }

  /** Charge les données une seule fois : API si elle répond, sinon démonstration */
  charger(): Observable<void> {
    return (this.pret$ ??= forkJoin({
      m: this.http.get<MedicamentSensible[]>('/api/medicaments-sensibles'),
      a: this.http.get<Attribution[]>('/api/attributions'),
      v: this.http.get<VenteSensible[]>('/api/ventes')
    }).pipe(
      timeout(3000),
      tap(({ m, a, v }) => {
        this.medicaments.set(m);
        this.attributions.set(a.map(x => this.deApi(x)));
        this.ventes.set(v.map(x => this.deApi(x)));
        this.enLigne.set(true);
      }),
      map(() => void 0),
      catchError(() => {
        this.enLigne.set(false);
        return of(void 0);
      }),
      shareReplay(1)
    ));
  }

  private demo() { return this.enLigne() !== true; }
  private erreur(status: number) { return throwError(() => ({ status })); }

  // ---------- Médicaments sensibles (CRUD) ----------
  creerMedicament(m: NouveauMedicament): Observable<MedicamentSensible> {
    if (!this.demo()) return this.http.post<MedicamentSensible>('/api/medicaments-sensibles', m).pipe(tap(c => this.medicaments.update(l => [...l, c])));
    if (this.medicaments().some(x => x.nomCommercial.toLowerCase() === m.nomCommercial.trim().toLowerCase())) return this.erreur(409);
    const cree = { ...m, id: Math.max(0, ...this.medicaments().map(x => x.id)) + 1 };
    this.medicaments.update(l => [...l, cree]);
    return of(cree);
  }

  modifierMedicament(id: number, m: NouveauMedicament): Observable<MedicamentSensible> {
    if (!this.demo()) return this.http.put<MedicamentSensible>(`/api/medicaments-sensibles/${id}`, m).pipe(tap(c => this.medicaments.update(l => l.map(x => (x.id === id ? c : x)))));
    if (this.medicaments().some(x => x.id !== id && x.nomCommercial.toLowerCase() === m.nomCommercial.trim().toLowerCase())) return this.erreur(409);
    const maj = { ...m, id };
    this.medicaments.update(l => l.map(x => (x.id === id ? maj : x)));
    return of(maj);
  }

  supprimerMedicament(id: number): Observable<void> {
    const local = () => {
      this.medicaments.update(l => l.filter(x => x.id !== id));
      this.attributions.update(l => l.filter(a => a.medicamentId !== id));
    };
    if (!this.demo()) return this.http.delete<void>(`/api/medicaments-sensibles/${id}`).pipe(tap(local));
    local();
    return of(void 0);
  }

  // ---------- Quotas (attributions) ----------
  attribuerLot(req: LotRequest): Observable<ResultatLot> {
    if (!this.demo()) {
      const corps = { medicamentId: req.medicamentId, mois: req.mois, quantiteAttribuee: req.quantite, pharmacieIds: req.pharmacieIds.map(versBack) };
      return this.http.post<{ attributions: Attribution[]; pharmaciesIgnorees: string[] }>('/api/attributions/lot', corps).pipe(
        map(r => ({ creees: r.attributions.map(x => this.deApi(x)), ignorees: r.pharmaciesIgnorees.map(i => depuisBack(i) as number) } as ResultatLot)),
        tap(r => this.attributions.update(l => [...l, ...r.creees]))
      );
    }
    const creees: Attribution[] = [], ignorees: number[] = [];
    let id = Math.max(0, ...this.attributions().map(a => a.id));
    req.pharmacieIds.forEach(pid => {
      const existe = this.attributions().some(a => a.pharmacieId === pid && a.medicamentId === req.medicamentId && a.mois === req.mois);
      if (existe) ignorees.push(pid);
      else creees.push({ id: ++id, pharmacieId: pid, medicamentId: req.medicamentId, mois: req.mois, quantiteAttribuee: req.quantite, quantiteVendue: 0, avertissements: 0, statut: 'ACTIVE' });
    });
    this.attributions.update(l => [...l, ...creees]);
    return of({ creees, ignorees });
  }

  modifierAttribution(id: number, quantiteAttribuee: number): Observable<Attribution> {
    const local = (a: Attribution) => this.attributions.update(l => l.map(x => (x.id === id ? a : x)));
    const actuelle = this.attributions().find(a => a.id === id)!;
    if (!this.demo()) {
      return this.http.put<Attribution>(`/api/attributions/${id}`, { quantiteAttribuee, statut: actuelle.statut }).pipe(map(a => this.deApi(a)), tap(local));
    }
    const maj = { ...actuelle, quantiteAttribuee };
    local(maj);
    return of(maj);
  }

  supprimerAttribution(id: number): Observable<void> {
    const local = () => this.attributions.update(l => l.filter(a => a.id !== id));
    if (!this.demo()) return this.http.delete<void>(`/api/attributions/${id}`).pipe(tap(local));
    local();
    return of(void 0);
  }

  bilan(id: number): Observable<Bilan> {
    const calcul = (): Bilan => {
      const a = this.attributions().find(x => x.id === id)!;
      return {
        attribuee: a.quantiteAttribuee, vendue: a.quantiteVendue, restant: Math.max(0, a.quantiteAttribuee - a.quantiteVendue),
        taux: a.quantiteAttribuee ? Math.round((a.quantiteVendue / a.quantiteAttribuee) * 100) : 0, avertissements: a.avertissements, statut: a.statut
      };
    };
    if (this.demo()) return of(calcul());
    return this.http.get<{ recu: number; vendu: number; reste: number; avertissements: number; statut: StatutAttribution }>(`/api/attributions/${id}/bilan`).pipe(
      map(b => ({ attribuee: b.recu, vendue: b.vendu, restant: b.reste, taux: b.recu ? Math.round((b.vendu / b.recu) * 100) : 0, avertissements: b.avertissements, statut: b.statut } as Bilan)),
      catchError(() => of(calcul()))
    );
  }

  // ---------- Disponibilités (pour la consultation) ----------
  disponibilites(medicamentId: number): Observable<Disponibilite[]> {
    const calcul = (): Disponibilite[] => this.attributionsDuMois()
      .filter(a => a.medicamentId === medicamentId)
      .map(a => {
        const restant = Math.max(0, a.quantiteAttribuee - a.quantiteVendue);
        const p = this.pharmacie(a.pharmacieId)!;
        const etat = a.statut === 'SUSPENDUE' || restant === 0 ? 'INDISPONIBLE' : restant / a.quantiteAttribuee <= 0.15 ? 'BIENTOT_EPUISE' : 'DISPONIBLE';
        return { pharmacieId: p.id, nom: p.nom, ville: p.ville, restant, etat } as Disponibilite;
      });
    if (this.demo()) return of(calcul());
    return this.http.get<{ pharmacieId: string; reste: number; etat: Disponibilite['etat'] }[]>('/api/disponibilites', { params: new HttpParams().set('medicamentId', medicamentId) }).pipe(
      map(liste => liste.map(d => {
        const id = this.deApi(d).pharmacieId as unknown as number;
        const p = this.pharmacie(id);
        return { pharmacieId: id, nom: p?.nom ?? `Pharmacie ${d.pharmacieId}`, ville: p?.ville ?? '', restant: d.reste, etat: d.etat } as Disponibilite;
      })),
      catchError(() => of(calcul()))
    );
  }

  // ---------- Déclarer une vente (RG1, RG2, RG3) ----------
  declarerVente(req: { ordonnanceCode: string; medicamentId: number; quantite: number }): Observable<ResultatVente> {
    const pid = this.pharmacieId();
    const etapes = (): EtapeVente[] => [
      { libelle: 'Pharmacie active', ok: null },
      { libelle: 'Ordonnance validée contenant ce médicament', ok: null },
      { libelle: 'Quota du mois suffisant', ok: null }
    ];

    if (!this.demo()) {
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

    // ----- mode démonstration : les mêmes règles que le back -----
    const e = etapes();
    const att = this.attributionsDuMois().find(a => a.pharmacieId === pid && a.medicamentId === req.medicamentId);
    if (this.statutPharmacie(pid) === 'SUSPENDUE') {
      e[0].ok = false;
      return of({ ok: false, code: 403, message: 'Votre pharmacie est suspendue : aucune vente de médicament sensible n\'est possible.', etapes: e, avertissement: false, suspendue: true });
    }
    e[0].ok = true;
    const faute = (message: string): ResultatVente => {
      let suspendue = false;
      if (att) {
        const n = att.avertissements + 1;
        suspendue = n >= 2;
        this.attributions.update(l => l.map(a => (a.id === att.id ? { ...a, avertissements: n, statut: suspendue ? 'SUSPENDUE' : a.statut } : a)));
      }
      return { ok: false, code: 409, message, etapes: e, avertissement: !!att, suspendue };
    };
    const o = ORDONNANCES.find(x => x.code.toLowerCase() === req.ordonnanceCode.trim().toLowerCase());
    if (!o || !o.validee || !o.medicaments.includes(req.medicamentId)) {
      e[1].ok = false;
      return of(faute(!o ? 'Ordonnance introuvable.' : !o.validee ? 'Cette ordonnance n\'est pas validée.' : 'Cette ordonnance ne contient pas ce médicament.'));
    }
    e[1].ok = true;
    if (!att || att.quantiteAttribuee - att.quantiteVendue < req.quantite) {
      e[2].ok = false;
      return of(faute(!att ? 'Aucun quota pour ce médicament ce mois-ci.' : `Quota épuisé : il reste ${att.quantiteAttribuee - att.quantiteVendue} boîte(s).`));
    }
    e[2].ok = true;
    const vente: VenteSensible = { id: Math.max(0, ...this.ventes().map(v => v.id)) + 1, patientId: o.patientId, ordonnanceId: o.id, quantite: req.quantite, date: new Date().toISOString().slice(0, 10), pharmacieId: pid, medicamentId: req.medicamentId };
    this.ventes.update(l => [vente, ...l]);
    this.attributions.update(l => l.map(a => (a.id === att.id ? { ...a, quantiteVendue: a.quantiteVendue + req.quantite } : a)));
    return of({ ok: true, code: 201, message: 'Vente enregistrée.', etapes: e, avertissement: false, suspendue: false, vente });
  }
}
