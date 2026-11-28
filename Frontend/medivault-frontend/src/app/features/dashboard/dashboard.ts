import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { Icon } from '../../shared/icon';
import { CountUp } from '../../shared/count-up';
import { DashboardSensiblesAdmin, DashboardSensiblesPharmacien } from '../medicaments/components/dashboard-sensibles';
import { AuthService } from '../../core/auth.service';
import { SensibleService } from '../medicaments/services/sensible.service';

interface Serie { label: string; mois: string[]; prescrites: number[]; delivrees: number[]; }

// Ce que renvoient les autres microservices (seulement les champs utiles au tableau de bord)
interface OrdonnanceApi { id: number; code: string; patientId: string; medecinId: string; statut: string; dateEmission: string; lignes: unknown[]; }
interface RendezVousApi { id: number; patientId: string; professionnelId: string; motif: string; statut: string; dateDebut: string; }
interface ResultatApi { parametre: string; valeur: number; unite: string; interpretation: string; }
interface AnalyseApi { id: number; patientId: string; medecinId: string; laboratoireId: string | null; typeAnalyse: string; statut: string; dateDemande: string; resultats: ResultatApi[]; }
interface UtilisateurApi { id: string; nom: string; prenom: string; role: string; nomEtablissement?: string; }

const MOIS_COURTS = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];
const SERVICES = ['medicament', 'ordonnance', 'dossier', 'soin-domicile', 'analyse', 'rendezvous', 'user'] as const;
type NomService = typeof SERVICES[number];

/** « 2026-11 » pour le mois d'une date ISO ou d'un objet Date */
const cleMois = (d: string | Date) => (typeof d === 'string' ? d.slice(0, 7) : `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
const cleJour = (d: string | Date) => (typeof d === 'string' ? d.slice(0, 10) : `${cleMois(d)}-${String(d.getDate()).padStart(2, '0')}`);

@Component({
  selector: 'mv-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, Icon, CountUp, DashboardSensiblesAdmin, DashboardSensiblesPharmacien],
  templateUrl: './dashboard.html'
})
export class DashboardPage {
  protected auth = inject(AuthService);
  private http = inject(HttpClient);
  private sensible = inject(SensibleService);
  protected role = computed(() => this.auth.utilisateur()?.role);
  private moi = computed(() => this.auth.utilisateur()?.id ?? '');

  protected aujourdhui = new Date();

  // ---------- Données réelles (chargées par la Gateway) ----------
  private ordonnancesApi = signal<OrdonnanceApi[]>([]);
  private rendezVousApi = signal<RendezVousApi[]>([]);
  private analysesApi = signal<AnalyseApi[]>([]);
  /** Noms affichés, lus dans user-service (id → « Ahmed Ben Ali ») */
  private noms = signal<Record<string, string>>({});
  /** true : le service répond · false : il ne répond pas · null : vérification en cours */
  private etats = signal<Record<NomService, boolean | null>>(
    Object.fromEntries(SERVICES.map(s => [s, null])) as Record<NomService, boolean | null>
  );

  protected salutation = computed(() => {
    const h = new Date().getHours();
    const debut = h < 18 ? 'Bonjour' : 'Bonsoir';
    const u = this.auth.utilisateur();
    const prefixe = u?.role === 'MEDECIN' ? 'Dr ' : '';
    return `${debut}, ${prefixe}${u?.nom.split(' ')[0] ?? ''}`;
  });

  // ---------- Ce que voit la personne connectée ----------
  private ordonnances$ = computed(() => {
    const r = this.role(), moi = this.moi();
    return this.ordonnancesApi().filter(o => r === 'PATIENT' ? o.patientId === moi : r === 'MEDECIN' ? o.medecinId === moi : true);
  });
  private rendezVous$ = computed(() => {
    const r = this.role(), moi = this.moi();
    return this.rendezVousApi().filter(v => r === 'PATIENT' ? v.patientId === moi : r === 'MEDECIN' || r === 'LABORANTIN' ? v.professionnelId === moi : true);
  });
  private analyses$ = computed(() => {
    const r = this.role(), moi = this.moi();
    return this.analysesApi().filter(a => r === 'PATIENT' ? a.patientId === moi : r === 'MEDECIN' ? a.medecinId === moi
      : r === 'LABORANTIN' ? a.laboratoireId === moi : true);
  });
  private refus$ = computed(() => this.role() === 'PHARMACIEN'
    ? this.sensible.refus().filter(t => t.pharmacieId === this.sensible.pharmacieId()) : this.sensible.refus());

  /** Les N derniers mois, du plus ancien au plus récent */
  private derniersMois(n: number) {
    const auj = new Date();
    return Array.from({ length: n }, (_, i) => new Date(auj.getFullYear(), auj.getMonth() - (n - 1 - i), 1));
  }
  private parMois(dates: string[], mois: Date[]) {
    return mois.map(m => dates.filter(d => d && cleMois(d) === cleMois(m)).length);
  }

  // ---------- Indicateurs ----------
  protected kpis = computed(() => {
    const sept = this.derniersMois(7);
    const ceMois = cleMois(new Date());

    // Patients suivis : tous les patients vus dans les ordonnances, rendez-vous et analyses
    const premiereFois = new Map<string, string>();
    const noter = (patientId: string, date: string) => {
      const avant = premiereFois.get(patientId);
      if (!avant || date < avant) premiereFois.set(patientId, date);
    };
    this.ordonnances$().forEach(o => noter(o.patientId, o.dateEmission));
    this.rendezVous$().forEach(v => noter(v.patientId, v.dateDebut));
    this.analyses$().forEach(a => noter(a.patientId, a.dateDemande));
    const nouveaux = [...premiereFois.values()].filter(d => cleMois(d) === ceMois).length;
    const cumul = sept.map(m => [...premiereFois.values()].filter(d => cleMois(d) <= cleMois(m)).length);

    // Ordonnances du mois, comparées au mois précédent
    const ordoParMois = this.parMois(this.ordonnances$().map(o => o.dateEmission), sept);
    const [precedent, courant] = ordoParMois.slice(-2);
    const evolution = precedent === 0 ? (courant > 0 ? 'nouveau' : '=') : this.pourcentage(courant, precedent);

    const quatrieme = this.role() === 'ADMIN' || this.role() === 'PHARMACIEN'
      ? this.kpiRefus()
      : this.kpiAnalyses(sept);

    return [
      { label: 'Patients suivis', valeur: premiereFois.size, delta: `+${nouveaux} ce mois`, icon: 'users', ton: 'from-aqua-300 to-aqua-500', points: cumul },
      { label: 'Ordonnances ce mois', valeur: courant, delta: evolution, icon: 'clipboard', ton: 'from-sky-300 to-sky-500', points: ordoParMois },
      { label: 'Médicaments sensibles', valeur: this.sensible.medicaments().length, delta: 'API', icon: 'pill', ton: 'from-iris-400 to-iris-500',
        points: this.parMois(this.sensible.ventes().map(v => v.date), sept) },
      quatrieme
    ];
  });

  private pourcentage(courant: number, precedent: number) {
    const p = Math.round(((courant - precedent) / precedent) * 100);
    return p === 0 ? '=' : `${p > 0 ? '+' : '−'}${Math.abs(p)} %`;
  }

  private kpiRefus() {
    const jours = Array.from({ length: 7 }, (_, i) => { const d = new Date(); d.setDate(d.getDate() - (6 - i)); return cleJour(d); });
    const parJour = jours.map(j => this.refus$().filter(t => cleJour(t.date) === j).length);
    const ceMois = this.refus$().filter(t => cleMois(t.date) === cleMois(new Date())).length;
    return { label: 'Ventes refusées ce mois', valeur: ceMois, delta: `${parJour.reduce((a, b) => a + b, 0)} sur 7 j`,
      icon: 'alert', ton: 'from-amber-300 to-orange-400', points: parJour };
  }

  private kpiAnalyses(mois: Date[]) {
    const enCours = this.analyses$().filter(a => a.statut === 'EN_ATTENTE' || a.statut === 'EN_COURS').length;
    const terminees = this.analyses$().filter(a => a.statut === 'TERMINEE').length;
    return { label: 'Analyses en cours', valeur: enCours, delta: `${terminees} terminée${terminees > 1 ? 's' : ''}`,
      icon: 'alert', ton: 'from-amber-300 to-orange-400', points: this.parMois(this.analyses$().map(a => a.dateDemande), mois) };
  }

  // ---------- Graphique : ordonnances prescrites et délivrées par mois ----------
  protected series = computed<Serie[]>(() => [6, 12].map(n => {
    const mois = this.derniersMois(n);
    const ordos = this.ordonnances$();
    return {
      label: `${n} mois`,
      mois: mois.map(m => MOIS_COURTS[m.getMonth()]),
      prescrites: this.parMois(ordos.map(o => o.dateEmission), mois),
      delivrees: this.parMois(ordos.filter(o => o.statut === 'DELIVREE' || o.statut === 'PARTIELLEMENT_DELIVREE').map(o => o.dateEmission), mois)
    };
  }));
  protected serieIndex = signal(0);
  protected serie = computed(() => this.series()[this.serieIndex()]);
  protected survol = signal<number | null>(null);

  private readonly W = 640;
  private readonly H = 220;
  protected graph = computed(() => {
    const s = this.serie();
    const max = Math.max(1, ...s.prescrites, ...s.delivrees) * 1.15;
    const pas = this.W / (s.mois.length - 1);
    const pt = (v: number, i: number) => [i * pas, this.H - (v / max) * this.H] as const;
    const courbe = (vals: number[]) => {
      const p = vals.map(pt);
      let d = `M${p[0][0]},${p[0][1]}`;
      for (let i = 1; i < p.length; i++) {
        const [x0, y0] = p[i - 1], [x1, y1] = p[i];
        const cx = (x0 + x1) / 2;
        d += ` C${cx},${y0} ${cx},${y1} ${x1},${y1}`;
      }
      return d;
    };
    const ligneA = courbe(s.prescrites);
    const ligneB = courbe(s.delivrees);
    return {
      ligneA, ligneB,
      aireA: `${ligneA} L${this.W},${this.H} L0,${this.H} Z`,
      aireB: `${ligneB} L${this.W},${this.H} L0,${this.H} Z`,
      points: s.prescrites.map((v, i) => ({ x: pt(v, i)[0], y: pt(v, i)[1], yb: pt(s.delivrees[i], i)[1] })),
      pas
    };
  });

  protected sparkline(points: number[]) {
    const max = Math.max(...points), min = Math.min(...points);
    return points
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${(i / (points.length - 1)) * 100},${28 - ((p - min) / (max - min || 1)) * 24}`)
      .join(' ');
  }

  // ---------- Ordonnances récentes ----------
  protected ordonnances = computed(() => [...this.ordonnances$()]
    .sort((a, b) => b.dateEmission.localeCompare(a.dateEmission) || b.id - a.id)
    .slice(0, 4)
    .map(o => ({
      code: o.code,
      patient: this.nom(o.patientId),
      date: o.dateEmission,
      lignes: o.lignes?.length ?? 0,
      ...this.statutOrdonnance(o.statut)
    })));

  private statutOrdonnance(statut: string) {
    switch (statut) {
      case 'VALIDEE': return { statut: 'VALIDÉE', ton: 'bg-emerald-50 text-emerald-700' };
      case 'DELIVREE': return { statut: 'DÉLIVRÉE', ton: 'bg-ink-100 text-ink-600' };
      case 'PARTIELLEMENT_DELIVREE': return { statut: 'PARTIELLE', ton: 'bg-sky-50 text-sky-700' };
      case 'EN_ATTENTE_VERIFICATION': return { statut: 'EN ATTENTE', ton: 'bg-amber-50 text-amber-700' };
      default: return { statut: statut.replace(/_/g, ' '), ton: 'bg-rose-50 text-rose-700' };
    }
  }

  // ---------- Alertes de sécurité (risques, quotas, refus, résultats anormaux) ----------
  protected alertes = computed(() => {
    const liste: { gravite: number; niveau: string; icon: string; ton: string; texte: string; detail: string }[] = [];
    const rose = 'border-rose-200 bg-rose-50/80 text-rose-700', ambre = 'border-amber-200 bg-amber-50/80 text-amber-700', ciel = 'border-sky-200 bg-sky-50/80 text-sky-700';
    const r = this.role();

    if (r === 'ADMIN' || r === 'PHARMACIEN') {
      const miens = r === 'PHARMACIEN' ? this.sensible.risques().filter(x => x.pharmacieId === this.sensible.pharmacieId()) : this.sensible.risques();
      miens.filter(x => x.niveau !== 'FAIBLE').forEach(x => liste.push({
        gravite: x.niveau === 'ELEVE' ? 3 : 2, niveau: x.niveau === 'ELEVE' ? 'RISQUE ÉLEVÉ' : 'RISQUE MOYEN',
        icon: x.niveau === 'ELEVE' ? 'octagon' : 'alert', ton: x.niveau === 'ELEVE' ? rose : ambre,
        texte: this.sensible.pharmacie(x.pharmacieId)?.nom ?? `Pharmacie ${x.pharmacieId}`, detail: x.raisons.join(' · ')
      }));
      if (r === 'ADMIN') {
        this.sensible.previsions().filter(p => p.niveau === 'EPUISE' || p.niveau === 'ALERTE').forEach(p => liste.push({
          gravite: p.niveau === 'EPUISE' ? 3 : 2, niveau: p.niveau === 'EPUISE' ? 'QUOTA ÉPUISÉ' : 'QUOTA',
          icon: 'alert', ton: p.niveau === 'EPUISE' ? rose : ambre, texte: p.medicamentNom, detail: p.message
        }));
      }
      [...this.refus$()].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 2).forEach(t => liste.push({
        gravite: 1, niveau: 'VENTE REFUSÉE', icon: 'shield', ton: ciel,
        texte: `${t.medicamentNom} · ${t.ordonnanceCode}`, detail: t.detail
      }));
    } else {
      // médecin, patient, laboratoire : les résultats d'analyses hors des valeurs de référence
      this.analyses$().forEach(a => a.resultats
        .filter(x => x.interpretation && x.interpretation !== 'NORMAL')
        .forEach(x => liste.push({
          gravite: 2, niveau: `RÉSULTAT ${x.interpretation === 'ELEVE' ? 'ÉLEVÉ' : x.interpretation}`, icon: 'alert', ton: ambre,
          texte: `${x.parametre} : ${x.valeur} ${x.unite}`, detail: `${a.typeAnalyse} · ${this.nom(a.patientId)}`
        })));
    }
    return liste.sort((a, b) => b.gravite - a.gravite).slice(0, 4);
  });

  // ---------- Agenda : les rendez-vous du jour, sinon les prochains ----------
  private rdvActifs = computed(() => this.rendezVous$().filter(v => v.statut !== 'ANNULE')
    .sort((a, b) => a.dateDebut.localeCompare(b.dateDebut)));
  private rdvDuJour = computed(() => this.rdvActifs().filter(v => cleJour(v.dateDebut) === cleJour(new Date())));
  protected titreAgenda = computed(() => this.rdvDuJour().length ? 'Agenda du jour' : 'Prochains rendez-vous');
  protected agenda = computed(() => {
    const maintenant = new Date().toISOString().slice(0, 16);
    const choix = this.rdvDuJour().length ? this.rdvDuJour() : this.rdvActifs().filter(v => v.dateDebut.slice(0, 16) >= maintenant);
    return choix.slice(0, 4).map(v => ({
      id: v.id,
      heure: v.dateDebut.slice(11, 16),
      jour: v.dateDebut.slice(0, 10),
      aujourdhui: cleJour(v.dateDebut) === cleJour(new Date()),
      // le patient voit avec qui il a rendez-vous ; les professionnels voient le patient
      patient: this.role() === 'PATIENT' ? this.nom(v.professionnelId) : this.nom(v.patientId),
      motif: v.motif,
      statut: v.statut === 'TERMINE' ? 'Terminé' : 'Confirmé',
      ton: v.statut === 'TERMINE' ? 'bg-ink-100 text-ink-500' : 'bg-sky-50 text-sky-700'
    }));
  });

  // ---------- État des 7 microservices (chacun interrogé par la Gateway) ----------
  protected services = computed(() => SERVICES.map(nom => ({
    nom, etat: nom === 'medicament' ? this.sensible.enLigne() : this.etats()[nom]
  })));
  protected enLigne = computed(() => this.services().filter(s => s.etat === true).length);

  protected nom(id: string) { return this.noms()[id] ?? id; }
  protected initiales(nom: string) {
    const mots = nom.replace(/^Dr\s+/, '').split(/[\s-]+/).filter(Boolean);
    return ((mots[0]?.[0] ?? '') + (mots[1]?.[0] ?? '')).toUpperCase() || '?';
  }

  constructor() {
    this.sensible.charger().subscribe();

    const liste = <T>(url: string): Observable<T[] | null> => this.http.get<T[]>(url).pipe(catchError(() => of(null)));
    forkJoin({
      o: liste<OrdonnanceApi>('/api/ordonnances'),
      r: liste<RendezVousApi>('/api/rendezvous'),
      a: liste<AnalyseApi>('/api/analyses')
    }).subscribe(({ o, r, a }) => {
      this.ordonnancesApi.set(o ?? []);
      this.rendezVousApi.set(r ?? []);
      this.analysesApi.set(a ?? []);
      this.etats.update(e => ({ ...e, ordonnance: o !== null, rendezvous: r !== null, analyse: a !== null }));
      this.chargerNoms();
      // dossier-service : un dossier inconnu répond 404, ce qui prouve que le service est joignable
      const patient = this.role() === 'PATIENT' ? this.moi() : (o?.[0]?.patientId ?? 'patient-inconnu');
      this.sonder('dossier', `/api/dossiers/patient/${encodeURIComponent(patient)}/allergies`);
    });
    this.sonder('soin-domicile', '/api/visites/statistiques');
    this.sonder('user', '/api/professionnels?type=PHARMACIEN');
  }

  /** Le service répond (même 404 ou 403) = en ligne ; 0 ou 5xx (la Gateway ne le trouve pas) = hors ligne */
  private sonder(nom: NomService, url: string) {
    this.http.get(url, { observe: 'response' }).pipe(
      map(() => true),
      catchError((e: HttpErrorResponse) => of(e.status > 0 && e.status < 500))
    ).subscribe(ok => this.etats.update(e => ({ ...e, [nom]: ok })));
  }

  /** Va chercher dans user-service les noms des patients et professionnels affichés */
  private chargerNoms() {
    const ids = new Set<string>();
    [...this.ordonnances$()].sort((a, b) => b.dateEmission.localeCompare(a.dateEmission) || b.id - a.id).slice(0, 4).forEach(o => ids.add(o.patientId));
    this.rdvActifs().slice(0, 8).forEach(v => { ids.add(v.patientId); ids.add(v.professionnelId); });
    this.analyses$().forEach(a => ids.add(a.patientId));
    [...ids].filter(id => id && !this.noms()[id]).forEach(id =>
      this.http.get<UtilisateurApi>(`/api/utilisateurs/${encodeURIComponent(id)}`).pipe(catchError(() => of(null))).subscribe(u => {
        if (!u) return;
        const affiche = u.nomEtablissement || `${u.role === 'MEDECIN' ? 'Dr ' : ''}${u.prenom} ${u.nom}`.trim();
        this.noms.update(n => ({ ...n, [id]: affiche }));
      })
    );
  }
}
