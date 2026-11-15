package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Le pharmacien accepte la demande et affecte un infirmier disponible de sa pharmacie. */
public record AcceptationRequest(
        @NotBlank String pharmacienId,
        @NotNull Long infirmierId) {
}
