export interface LigneOrdonnance {
  medicamentId: number;
  nomMedicament: string;
  posologie: string;
  dureeJours: number;
  quantite: number;
}

export interface OrdonnanceRequest {
  /** Identifiants user-service (ex. patient-ahmed, medecin-karim) */
  patientId: string;
  medecinId: string;
  consultationId?: number;
  lignes: LigneOrdonnance[];
}

export interface Ordonnance extends OrdonnanceRequest {
  id: number;
  code: string;
  dateEmission: string;
  dateExpiration: string;
  statut: string;
  statutModifieParPharmacien: boolean;
  lignes: LigneOrdonnance[];
}
