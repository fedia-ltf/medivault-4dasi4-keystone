// ===========================================================================
// Modèles du medicament-service : médicaments sensibles, quotas (attributions), ventes.
// Les noms des champs sont exactement ceux de l'API.
// ===========================================================================

export type Categorie = 'STUPEFIANT' | 'PSYCHOTROPE' | 'ABORTIF';
export type StatutAttribution = 'ACTIVE' | 'SUSPENDUE';

export interface MedicamentSensible {
  id: number;
  nomCommercial: string;
  dci: string;
  categorie: Categorie;
  description: string;
  precautions: string;
}
export type NouveauMedicament = Omit<MedicamentSensible, 'id'>;

/** Quota d'un médicament pour une pharmacie et un mois */
export interface Attribution {
  id: number;
  pharmacieId: number;
  medicamentId: number;
  /** Mois au format AAAA-MM (YearMonth) */
  mois: string;
  quantiteAttribuee: number;
  quantiteVendue: number;
  avertissements: number;
  statut: StatutAttribution;
}

export interface VenteSensible {
  id: number;
  /** Identifiant du patient dans user-service (ex. patient-ahmed) */
  patientId: string;
  ordonnanceId: number;
  quantite: number;
  /** Date ISO (AAAA-MM-JJ) */
  date: string;
  /** Renvoyés par GET /ventes pour pouvoir filtrer et afficher les lignes */
  pharmacieId: number;
  medicamentId: number;
  /** Code de l'ordonnance, renvoyé par le back */
  ordonnanceCode?: string;
}

/** Les pharmacies appartiennent au pharmacie-service : ici seulement de quoi les afficher */
export interface Pharmacie { id: number; nom: string; ville: string; }

export interface LotRequest { medicamentId: number; mois: string; quantite: number; pharmacieIds: number[]; }
export interface ResultatLot { creees: Attribution[]; ignorees: number[]; }

export type EtatDisponibilite = 'DISPONIBLE' | 'BIENTOT_EPUISE' | 'INDISPONIBLE';
export interface Disponibilite { pharmacieId: number; nom: string; ville: string; restant: number; etat: EtatDisponibilite; }

export interface Bilan {
  attribuee: number; vendue: number; restant: number; taux: number; avertissements: number; statut: StatutAttribution;
}

/** Une étape du contrôle d'une vente : ok = null tant qu'elle n'a pas été évaluée */
export interface EtapeVente { libelle: string; ok: boolean | null; }
export interface ResultatVente {
  ok: boolean;
  /** 201 vente enregistrée · 403 pharmacie inactive · 409 ordonnance invalide ou quota épuisé · 503 service indisponible */
  code: number;
  message: string;
  etapes: EtapeVente[];
  avertissement: boolean;
  suspendue: boolean;
  vente?: VenteSensible;
}

export interface InfoCategorie {
  cle: Categorie;
  label: string;
  pluriel: string;
  icone: string;
  phrase: string;
  regle: string;
  badge: string;
}

export const CATEGORIES: InfoCategorie[] = [
  {
    cle: 'STUPEFIANT', label: 'Stupéfiant', pluriel: 'Stupéfiants', icone: 'syringe',
    phrase: 'Fort potentiel de dépendance : opioïdes et traitements de substitution.',
    regle: 'Substance classée stupéfiant : délivrance strictement encadrée.',
    badge: 'bg-aqua-50 text-aqua-700 ring-aqua-200'
  },
  {
    cle: 'PSYCHOTROPE', label: 'Psychotrope', pluriel: 'Psychotropes', icone: 'activity',
    phrase: 'Agit sur le système nerveux : anxiolytiques, somnifères, stimulants.',
    regle: 'Substance psychotrope : risque d\'usage détourné, contrôle des quantités.',
    badge: 'bg-teal-50 text-teal-700 ring-teal-200'
  },
  {
    cle: 'ABORTIF', label: 'Abortif', pluriel: 'Abortifs', icone: 'shield',
    phrase: 'Provoque ou facilite une interruption de grossesse, sous contrôle médical strict.',
    regle: 'Substance abortive : réservée aux situations médicales encadrées.',
    badge: 'bg-sky-50 text-sky-700 ring-sky-200'
  }
];
export const categorieInfo = (c: Categorie) => CATEGORIES.find(x => x.cle === c)!;

export const moisDe = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
export const libelleMois = (m: string) =>
  new Date(+m.slice(0, 4), +m.slice(5, 7) - 1, 1).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });

// ---------------------------------------------------------------------------
// Surveillance : score de risque, historique des refus, prévision d'épuisement
// ---------------------------------------------------------------------------
export type NiveauRisque = 'FAIBLE' | 'MOYEN' | 'ELEVE';
/** GET /risques : le score (0 à 100) d'une pharmacie, avec les raisons qui l'expliquent */
export interface RisquePharmacie {
  pharmacieId: number; score: number; niveau: NiveauRisque; refus: number; avertissements: number;
  suspensions: number; tauxConsommation: number; raisons: string[];
}

export type MotifRefus = 'PHARMACIE_INACTIVE' | 'AUCUN_QUOTA' | 'PHARMACIE_SUSPENDUE' | 'ORDONNANCE_NON_JUSTIFIEE' | 'QUOTA_EPUISE';
/** GET /tentatives-refusees : une vente refusée (qui, quoi, quand, pourquoi) */
export interface TentativeRefusee {
  id: number; pharmacieId: number; medicamentId: number; medicamentNom: string; ordonnanceCode: string;
  quantite: number; date: string; motif: MotifRefus; detail: string;
}

export type NiveauPrevision = 'OK' | 'A_SURVEILLER' | 'ALERTE' | 'EPUISE';
/** GET /previsions/medicaments : « à ce rythme, le quota sera épuisé vers le 23 » */
export interface PrevisionMedicament {
  medicamentId: number; medicamentNom: string; pharmacies: number; quantiteAttribuee: number; quantiteVendue: number;
  reste: number; pourcentageUtilise: number; dateEpuisementPrevue: string | null; epuisementAvantFinDeMois: boolean;
  niveau: NiveauPrevision; pharmaciesEnAlerte: number; message: string;
}
