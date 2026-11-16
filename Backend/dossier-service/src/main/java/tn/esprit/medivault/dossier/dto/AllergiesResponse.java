package tn.esprit.medivault.dossier.dto;

import java.util.List;

public record AllergiesResponse(
        String patientId,
        List<String> allergies
) {}
