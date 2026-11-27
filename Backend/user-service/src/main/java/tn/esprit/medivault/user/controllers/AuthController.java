package tn.esprit.medivault.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.user.dto.AuthDtos.ChangerMotDePasseRequest;
import tn.esprit.medivault.user.dto.AuthDtos.InscriptionRequest;
import tn.esprit.medivault.user.dto.AuthDtos.LoginRequest;
import tn.esprit.medivault.user.dto.AuthDtos.MessageResponse;
import tn.esprit.medivault.user.dto.AuthDtos.MotDePasseOublieRequest;
import tn.esprit.medivault.user.dto.AuthDtos.RefreshRequest;
import tn.esprit.medivault.user.dto.AuthDtos.ReinitialiserMotDePasseRequest;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.exceptions.IdentifiantsInvalidesException;
import tn.esprit.medivault.user.services.IAuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Connexion", description = "Connexion par Keycloak, inscription d'un patient, mot de passe oublié")
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Se connecter (email + mot de passe) : renvoie les jetons JWT et le profil avec son rôle")
    @ApiResponse(responseCode = "200", description = "Connecté")
    @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect")
    @ApiResponse(responseCode = "403", description = "Compte désactivé ou sans profil MediVault")
    @ApiResponse(responseCode = "503", description = "Keycloak indisponible ou désactivé")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.connexion(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler la session avec le refreshToken")
    @ApiResponse(responseCode = "200", description = "Nouveaux jetons")
    @ApiResponse(responseCode = "401", description = "Session expirée : se reconnecter")
    public SessionResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.renouveler(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Se déconnecter (ferme la session Keycloak)")
    @ApiResponse(responseCode = "204", description = "Déconnecté")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.deconnexion(request.refreshToken());
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "S'inscrire : crée un compte PATIENT (Keycloak + MongoDB). Les professionnels sont créés par l'administrateur")
    @ApiResponse(responseCode = "201", description = "Compte créé : on peut se connecter")
    @ApiResponse(responseCode = "400", description = "Données invalides (mot de passe trop faible, email invalide…)")
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    @ApiResponse(responseCode = "503", description = "Keycloak indisponible")
    public UtilisateurResponse inscription(@Valid @RequestBody InscriptionRequest request) {
        return authService.inscription(request);
    }

    @PostMapping("/mot-de-passe-oublie")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Mot de passe oublié : envoie un e-mail avec un lien pour en choisir un nouveau (même réponse si le compte n'existe pas)")
    @ApiResponse(responseCode = "202", description = "Demande prise en compte")
    public MessageResponse motDePasseOublie(@Valid @RequestBody MotDePasseOublieRequest request) {
        return authService.motDePasseOublie(request.email());
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    @Operation(summary = "Choisir un nouveau mot de passe avec le lien reçu par e-mail (lien à usage unique)")
    @ApiResponse(responseCode = "200", description = "Mot de passe modifié")
    @ApiResponse(responseCode = "400", description = "Lien invalide ou expiré, ou mot de passe trop faible")
    @ApiResponse(responseCode = "403", description = "Compte désactivé")
    public MessageResponse reinitialiser(@Valid @RequestBody ReinitialiserMotDePasseRequest request) {
        return authService.reinitialiserMotDePasse(request.token(), request.nouveauMotDePasse());
    }

    @GetMapping("/moi")
    @Operation(summary = "Mon profil (d'après le jeton JWT)")
    @ApiResponse(responseCode = "200", description = "Profil de la personne connectée")
    @ApiResponse(responseCode = "401", description = "Non connecté")
    public UtilisateurResponse moi(@AuthenticationPrincipal Jwt jwt) {
        return authService.moi(courriel(jwt));
    }

    @PostMapping("/changer-mot-de-passe")
    @Operation(summary = "Changer mon mot de passe (il faut donner l'ancien)")
    @ApiResponse(responseCode = "200", description = "Mot de passe modifié")
    @ApiResponse(responseCode = "401", description = "Non connecté ou mot de passe actuel incorrect")
    public MessageResponse changerMotDePasse(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangerMotDePasseRequest request) {
        return authService.changerMotDePasse(courriel(jwt), request.ancienMotDePasse(), request.nouveauMotDePasse());
    }

    /** L'email de la personne connectée, lu dans son jeton JWT. */
    private static String courriel(Jwt jwt) {
        if (jwt == null) {
            throw new IdentifiantsInvalidesException("Vous n'êtes pas connecté.");
        }
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getClaimAsString("preferred_username");
    }
}
