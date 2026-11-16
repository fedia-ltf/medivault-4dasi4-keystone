package tn.esprit.medivault.dossier.dto;

import tn.esprit.medivault.dossier.entities.GroupeSanguin;

import java.util.List;

public record DossierResponse(
        Long id,
        String patientId,
        GroupeSanguin groupeSanguin,
        List<String> allergies,
        String antecedents,
        List<ConsultationResponse> consultations
) {}
