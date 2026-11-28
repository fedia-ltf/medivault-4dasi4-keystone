package tn.esprit.medivault.dossier.dto;

public record AccesJustificationResponse(
        String patientId,
        String professionnelId,
        boolean autorise
) {}
