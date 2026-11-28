package tn.esprit.medivault.dossier.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConsultationRequest(
        @NotBlank(message = "L'identifiant du médecin est obligatoire")
        String medecinId,

        @NotBlank(message = "Le motif est obligatoire")
        String motif,

        String diagnostic,
        String observations,
        List<String> analysesDemandees
) {}
