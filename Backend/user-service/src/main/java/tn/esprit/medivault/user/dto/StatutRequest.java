package tn.esprit.medivault.user.dto;

import jakarta.validation.constraints.NotNull;

/** Activation ou désactivation d'un compte par l'administrateur. */
public record StatutRequest(@NotNull(message = "actif est obligatoire") Boolean actif) {
}
