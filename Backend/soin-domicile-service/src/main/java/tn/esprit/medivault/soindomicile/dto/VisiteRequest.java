package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.*;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import java.time.LocalDateTime;

public record VisiteRequest(
        @NotBlank String patientId,
        @NotBlank String pharmacieId,
        @NotNull @Future LocalDateTime date,
        @NotBlank String adresse,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull TypeSoin typeSoin,
        Long ordonnanceId,
        Long medicamentId,
        boolean medicamentSensible,
        String dose) {
}
