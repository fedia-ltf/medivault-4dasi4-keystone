package tn.esprit.medivault.analyse.dto;

/** Réponse de dossier-service : GET /api/dossiers/patient/{patientId}/acces?professionnelId= */
public record AccesResponse(Long patientId, Long professionnelId, boolean autorise) {
}
