package tn.esprit.medivault.analyse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Saisie du résultat par le laboratoire. laboratoireId identifie le laboratoire qui saisit
 * (il sera lu dans le jeton Keycloak en séance 8).
 */
public record ResultatRequest(
        @Schema(example = "labo-biolab") @NotBlank(message = "laboratoireId est obligatoire") String laboratoireId,
        @NotEmpty(message = "au moins un résultat est obligatoire") List<@Valid LigneResultatRequest> resultats
) {
}
