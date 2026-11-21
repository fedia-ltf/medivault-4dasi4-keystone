package tn.esprit.medivault.analyse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Un paramètre mesuré : la valeur et l'intervalle de référence servent à calculer l'interprétation. */
public record LigneResultatRequest(
        @Schema(example = "Glycémie") @NotBlank(message = "le paramètre est obligatoire") String parametre,
        @Schema(example = "1.30") @NotNull(message = "la valeur est obligatoire") Double valeur,
        @Schema(example = "g/L") @NotBlank(message = "l'unité est obligatoire") String unite,
        @Schema(example = "0.70") @NotNull(message = "la borne basse de référence est obligatoire") Double referenceMin,
        @Schema(example = "1.10") @NotNull(message = "la borne haute de référence est obligatoire") Double referenceMax
) {
}
