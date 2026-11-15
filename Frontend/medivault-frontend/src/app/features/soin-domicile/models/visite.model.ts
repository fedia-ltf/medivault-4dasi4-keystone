// Mêmes noms que le JSON du soin-domicile-service (diagramme de classes)
export type TypeSoin = 'INJECTION' | 'MESURE_TENSION' | 'GLYCEMIE' | 'PRISE_DE_SANG';
export type StatutVisite = 'DEMANDEE' | 'ACCEPTEE' | 'REALISEE' | 'ANNULEE';
export type Frequence = 'QUOTIDIENNE' | 'HEBDOMADAIRE' | 'MENSUELLE';

export interface Visite {
  id: number;
  patientId: string;
  pharmacieId: string;
  pharmacienId?: string;
  infirmierNom?: string;          // infirmier affecté par le pharmacien (pas de compte)
  infirmierTelephone?: string;
  date: string;
  adresse: string;
  latitude?: number;              // position GPS envoyée par le patient
  longitude?: number;
  typeSoin: TypeSoin;
  ordonnanceId?: number;
  statut: StatutVisite;
  tension?: string;
  glycemie?: number;
  injectionFaite: boolean;
  dose?: string;
  observations?: string;
  dateRapport?: string;
  medicamentId?: number;
  medicamentSensible: boolean;
}

export interface VisiteRequest {
  patientId: string;
  pharmacieId: string;
  date: string;
  adresse: string;
  latitude?: number;
  longitude?: number;
  typeSoin: TypeSoin;
  ordonnanceId?: number;
  medicamentId?: number;
  medicamentSensible: boolean;
  dose?: string;
}

export interface AcceptationRequest {
  pharmacienId: string;
  infirmierId: number;
}

// Infirmier d'une pharmacie, géré par le pharmacien (pas de compte)
export interface Infirmier {
  id: number;
  prenom: string;
  nom: string;
  nomComplet: string;
  telephone?: string;
  heureDebut: string;             // "08:00:00" : début de disponibilité
  heureFin: string;               // "16:00:00" : fin de disponibilité
  pharmacieId: string;
}

export type InfirmierRequest = Omit<Infirmier, 'id' | 'nomComplet'>;

/** Lien OpenStreetMap vers la position du patient. */
export function lienCarte(v: { latitude?: number; longitude?: number }): string | null {
  return v.latitude != null && v.longitude != null
    ? `https://www.openstreetmap.org/?mlat=${v.latitude}&mlon=${v.longitude}#map=17/${v.latitude}/${v.longitude}`
    : null;
}

export interface RapportRequest {
  pharmacienId: string;
  tension?: string;
  glycemie?: number;
  injectionFaite?: boolean;
  dose?: string;
  observations?: string;
}

export interface PlanSuivi {
  id: number;
  patientId: string;
  medecinId: string;
  pharmacieId: string;
  adresse: string;
  typeSoin: TypeSoin;
  frequence: Frequence;
  dateDebut: string;
  dateFin: string;
  heureVisite: string;
  seuilAlerte?: string;
}

export type PlanSuiviRequest = Omit<PlanSuivi, 'id'>;

export const TYPES_SOIN: { valeur: TypeSoin; libelle: string }[] = [
  { valeur: 'INJECTION', libelle: 'Injection' },
  { valeur: 'MESURE_TENSION', libelle: 'Mesure de tension' },
  { valeur: 'GLYCEMIE', libelle: 'Glycémie' },
  { valeur: 'PRISE_DE_SANG', libelle: 'Prise de sang' }
];

export const FREQUENCES: { valeur: Frequence; libelle: string }[] = [
  { valeur: 'QUOTIDIENNE', libelle: 'Chaque jour' },
  { valeur: 'HEBDOMADAIRE', libelle: 'Chaque semaine' },
  { valeur: 'MENSUELLE', libelle: 'Chaque mois' }
];

// Pharmacies de démonstration (le pharmacien connecté travaille à la première)
export const PHARMACIES: { id: string; nom: string }[] = [
  { id: 'ph-ennasr', nom: 'Pharmacie Ennasr' },
  { id: 'ph-centrale', nom: 'Pharmacie Centrale' },
  { id: 'ph-lac', nom: 'Pharmacie du Lac' }
];
export const PHARMACIE_DU_PHARMACIEN = 'ph-ennasr';

// Libellé et couleur de chaque statut
export const STATUTS: Record<StatutVisite, { libelle: string; ton: string }> = {
  DEMANDEE: { libelle: 'En attente de confirmation', ton: 'bg-amber-50 text-amber-700' },
  ACCEPTEE: { libelle: 'Confirmé', ton: 'bg-aqua-50 text-aqua-700' },
  REALISEE: { libelle: 'Soin réalisé', ton: 'bg-emerald-50 text-emerald-700' },
  ANNULEE: { libelle: 'Annulé', ton: 'bg-ink-100 text-ink-500' }
};

export function libelleSoin(t: TypeSoin): string {
  return TYPES_SOIN.find(x => x.valeur === t)?.libelle ?? t;
}

export function nomPharmacie(id: string): string {
  return PHARMACIES.find(p => p.id === id)?.nom ?? id;
}

export function ordonnanceObligatoire(t: TypeSoin): boolean {
  return t === 'INJECTION' || t === 'PRISE_DE_SANG';
}

/** RG4, même règle que Visite.estAnormale() côté backend : seuil "tension>14/9;glycemie>1.8". */
export function estAnormale(v: Visite, seuil?: string): boolean {
  if (!seuil) return false;
  for (const partie of seuil.replace(/\s/g, '').toLowerCase().split(';')) {
    if (partie.startsWith('tension>') && v.tension) {
      const [lh, lb] = partie.substring(8).split('/').map(Number);
      const [mh, mb] = v.tension.split('/').map(Number);
      if (mh > lh || mb > lb) return true;
    }
    if (partie.startsWith('glycemie>') && v.glycemie != null && v.glycemie > Number(partie.substring(9).replace(',', '.'))) {
      return true;
    }
  }
  return false;
}
