package tn.esprit.medivault.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tn.esprit.medivault.user.entities.Role;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Création d'un compte. Les champs professionnels sont obligatoires selon le rôle
 * (contrôlés dans le service) et ignorés pour un PATIENT ou un ADMIN.
 */
public record UtilisateurRequest(
        @NotBlank(message = "le nom est obligatoire") @Size(max = 60) String nom,
        @NotBlank(message = "le prénom est obligatoire") @Size(max = 60) String prenom,
        @NotBlank(message = "l'email est obligatoire") @Email(message = "email invalide") String email,
        @Pattern(regexp = "^\\+?[0-9 ]{8,15}$", message = "téléphone invalide") String telephone,
        @Schema(description = "Mot de passe initial (utilisé seulement si Keycloak est activé)")
        @Size(min = 8, message = "8 caractères minimum") String motDePasse,
        @NotNull(message = "le rôle est obligatoire") Role role,
        String nomEtablissement,
        String numeroAgrement,
        String adresse,
        String ville,
        List<DayOfWeek> joursTravail,
        @Schema(type = "string", example = "08:00") LocalTime heureDebut,
        @Schema(type = "string", example = "17:00") LocalTime heureFin
) {
}
