// Modèles TypeScript pour le module Dossiers médicaux (alignés sur le microservice dossier-service, port 8085)

/**
 * Consultation médicale rattachée à un dossier.
 * Les identifiants (patientId, medecinId) sont strictement des String.
 */
export interface Consultation {
  id: number;
  medecinId: string;
  date: string;
  motif: string;
  diagnostic: string;
  observations: string;
  analysesDemandees: string[];
}

/**
 * Dossier médical d'un patient.
 */
export interface Dossier {
  id: number;
  patientId: string;
  groupeSanguin: string;
  allergies: string[];
  antecedents: string;
  consultations: Consultation[];
}

/**
 * Données requises pour le formulaire de création d'une consultation (POST /dossiers/{id}/consultations)
 */
export interface NouvelleConsultation {
  medecinId: string;
  motif: string;
  diagnostic: string;
  observations: string;
  analysesDemandees: string[];
}

/**
 * Données requises pour la modification d'une consultation (PUT /dossiers/{id}/consultations/{consultationId})
 */
export interface ConsultationUpdateRequest {
  medecinId: string;
  motif: string;
  diagnostic: string;
  observations: string;
}

/**
 * Réponse de l'endpoint GET /dossiers/patient/{patientId}/allergies
 */
export interface AllergiesResponse {
  patientId: string;
  allergies: string[];
}

/**
 * Réponse de l'endpoint GET /dossiers/patient/{patientId}/acces?professionnelId=
 * et PATCH /dossiers/{id}/acces/{professionnelId}
 */
export interface AccesDossierResponse {
  id?: number;
  patientId?: string;
  professionnelId: string;
  autorise?: boolean;
  statut?: string;
  dateAccord?: string;
  dateRetrait?: string;
}

/**
 * Format standard des erreurs RFC 9457 (Problem Details)
 */
export interface ProblemDetails {
  title?: string;
  detail?: string;
  status?: number;
}

/**
 * Enveloppe paginée générique renvoyée par GET /dossiers/{id}/consultations
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Paramètres de filtre pour la liste paginée et l'export PDF
 */
export interface FiltresConsultation {
  medecinId?: string;
  du?: string;
  au?: string;
}
