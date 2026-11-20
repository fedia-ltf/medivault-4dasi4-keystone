package tn.esprit.medivault.analyse.dto;

/** Réponse de dossier-service : GET /api/dossiers/patient/{patientId}/acces?professionnelId= */
public record AccesResponse(String patientId, String professionnelId, boolean autorise) {
}
