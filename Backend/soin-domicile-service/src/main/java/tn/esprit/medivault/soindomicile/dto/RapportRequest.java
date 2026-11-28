package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.*;

public record RapportRequest(
        @NotBlank String pharmacienId,
        @Pattern(regexp = "\\d{1,2}/\\d{1,2}", message = "format 13/8") String tension,
        @Positive Double glycemie,
        Boolean injectionFaite,
        String dose,
        @Size(max = 1000) String observations) {
}
