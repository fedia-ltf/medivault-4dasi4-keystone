// Modèles de données du rendezvous-service (correspond aux DTOs Spring Boot)

export interface Creneau {
  id: number;
  professionnelId: string;
  typeProfessionnel: 'MEDECIN' | 'LABORANTIN';
  dateDebut: string;
  dateFin: string;
  libre: boolean;
}

export interface RendezVous {
  id: number;
  patientId: string;
  motif: string;
  statut: 'CONFIRME' | 'ANNULE' | 'TERMINE';
  analyseId: number | null;
  creneauId: number;
  professionnelId: string;
  typeProfessionnel: 'MEDECIN' | 'LABORANTIN';
  dateDebut: string;
  dateFin: string;
}

export interface RendezVousRequest {
  creneauId: number;
  patientId: string;
  motif: string;
  analyseId?: number | null;
}

export interface CreneauRequest {
  professionnelId?: string;
  typeProfessionnel?: 'MEDECIN' | 'LABORANTIN';
  dateDebut: string;
  dateFin: string;
}
