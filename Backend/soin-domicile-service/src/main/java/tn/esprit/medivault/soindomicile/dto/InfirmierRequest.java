package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/** Le pharmacien ajoute ou modifie un infirmier de sa pharmacie. */
public record InfirmierRequest(
        @NotBlank @Size(max = 60) String prenom,
        @NotBlank @Size(max = 60) String nom,
        @Pattern(regexp = "^$|^[+0-9 ]{8,20}$", message = "numéro invalide") String telephone,
        @NotNull LocalTime heureDebut,
        @NotNull LocalTime heureFin,
        @NotBlank String pharmacieId) {
}
