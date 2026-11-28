package tn.esprit.medivault.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Les objets échangés par les routes de connexion (/api/auth/**).
public final class AuthDtos {

    private AuthDtos() {
    }

    // Une lettre et un chiffre au moins, 8 caractères au minimum (la même règle que la politique de mot de passe de Keycloak)
    private static final String MOT_DE_PASSE = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    private static final String MESSAGE_MOT_DE_PASSE = "le mot de passe doit contenir au moins une lettre et un chiffre";

    public record LoginRequest(
            @NotBlank(message = "l'email est obligatoire") @Email(message = "email invalide") String email,
            @NotBlank(message = "le mot de passe est obligatoire") String motDePasse) {
    }

    /** Ce que le front reçoit après la connexion : les jetons Keycloak et le profil (avec son rôle). */
    public record SessionResponse(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn,
                                  String tokenType, UtilisateurResponse utilisateur) {
    }

    public record RefreshRequest(@NotBlank(message = "le refreshToken est obligatoire") String refreshToken) {
    }

    /** Inscription libre : seulement un patient (les professionnels sont créés par l'administrateur). */
    public record InscriptionRequest(
            @NotBlank(message = "le nom est obligatoire") @Size(max = 60) String nom,
            @NotBlank(message = "le prénom est obligatoire") @Size(max = 60) String prenom,
            @NotBlank(message = "l'email est obligatoire") @Email(message = "email invalide") String email,
            @Pattern(regexp = "^\\+?[0-9 ]{8,15}$", message = "téléphone invalide") String telephone,
            @NotBlank(message = "le mot de passe est obligatoire")
            @Size(min = 8, max = 72, message = "8 caractères minimum")
            @Pattern(regexp = MOT_DE_PASSE, message = MESSAGE_MOT_DE_PASSE) String motDePasse) {
    }

    /** « canal » est facultatif : EMAIL (par défaut) ou WHATSAPP (le lien est envoyé au téléphone du compte). */
    public record MotDePasseOublieRequest(
            @NotBlank(message = "l'email est obligatoire") @Email(message = "email invalide") String email,
            @Pattern(regexp = "^(?i)(EMAIL|WHATSAPP)$", message = "canal invalide : EMAIL ou WHATSAPP") String canal) {
    }

    /** Le lien reçu par e-mail (« token » dans l'adresse) et le nouveau mot de passe choisi. */
    public record ReinitialiserMotDePasseRequest(
            @NotBlank(message = "le lien est obligatoire") String token,
            @NotBlank(message = "le nouveau mot de passe est obligatoire")
            @Size(min = 8, max = 72, message = "8 caractères minimum")
            @Pattern(regexp = MOT_DE_PASSE, message = MESSAGE_MOT_DE_PASSE) String nouveauMotDePasse) {
    }

    public record ChangerMotDePasseRequest(
            @NotBlank(message = "le mot de passe actuel est obligatoire") String ancienMotDePasse,
            @NotBlank(message = "le nouveau mot de passe est obligatoire")
            @Size(min = 8, max = 72, message = "8 caractères minimum")
            @Pattern(regexp = MOT_DE_PASSE, message = MESSAGE_MOT_DE_PASSE) String nouveauMotDePasse) {
    }

    public record MessageResponse(String message) {
    }
}
