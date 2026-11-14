export interface LigneOrdonnance {
  medicamentId: number;
  nomMedicament: string;
  posologie: string;
  dureeJours: number;
  quantite: number;
}

export interface OrdonnanceRequest {
  patientId: number;
  medecinId: number;
  consultationId?: number;
  lignes: LigneOrdonnance[];
}

export interface Ordonnance extends OrdonnanceRequest {
  id: number;
  code: string;
  dateEmission: string;
  dateExpiration: string;
  statut: string;
  lignes: LigneOrdonnance[];
}
