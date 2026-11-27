package tn.esprit.medivault.user.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.medivault.user.clients.EmailApiClient;
import tn.esprit.medivault.user.clients.KeycloakAdminClient;
import tn.esprit.medivault.user.entities.JetonReinitialisation;
import tn.esprit.medivault.user.exceptions.LienInvalideException;
import tn.esprit.medivault.user.repositories.JetonReinitialisationRepository;
import tn.esprit.medivault.user.dto.AuthDtos.InscriptionRequest;
import tn.esprit.medivault.user.dto.AuthDtos.LoginRequest;
import tn.esprit.medivault.user.dto.AuthDtos.MessageResponse;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurRequest;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.Utilisateur;
import tn.esprit.medivault.user.exceptions.CompteInaccessibleException;
import tn.esprit.medivault.user.exceptions.IdentifiantsInvalidesException;
import tn.esprit.medivault.user.exceptions.KeycloakIndisponibleException;
import tn.esprit.medivault.user.exceptions.UtilisateurNotFoundException;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Connexion par Keycloak. Le mot de passe passe uniquement par Keycloak (il n'est jamais enregistré dans MongoDB) :
 * 1. Keycloak vérifie l'email et le mot de passe et renvoie les jetons JWT (le rôle est dans le jeton) ;
 * 2. on retrouve le profil MediVault dans MongoDB (par l'email) et on refuse un compte désactivé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final KeycloakAdminClient keycloak;
    private final UtilisateurRepository utilisateurRepository;
    private final IUtilisateurService utilisateurService;
    private final ObjectMapper objectMapper;
    private final JetonReinitialisationRepository jetonRepository;
    private final EmailApiClient emailApi;

    /** Page où Keycloak renvoie la personne après avoir changé son mot de passe (flux Keycloak / Mailpit). */
    @Value("${medivault.auth.reset-redirect-uri:http://localhost:4301/login}")
    private String urlRetourApresReinitialisation;

    /** Page du front qui reçoit le lien envoyé par e-mail (flux par API d'e-mail) : <url>?token=... */
    @Value("${medivault.auth.reset-front-url:http://localhost:4301/reinitialiser-mot-de-passe}")
    private String urlPageReinitialisation = "http://localhost:4301/reinitialiser-mot-de-passe";

    @Value("${medivault.auth.reset-minutes:30}")
    private long dureeLienMinutes = 30;

    private final SecureRandom hasard = new SecureRandom();

    @Override
    public SessionResponse connexion(LoginRequest request) {
        String email = normaliser(request.email());
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(email).orElse(null);
        if (compte != null && !compte.isActif()) {
            throw new CompteInaccessibleException("Ce compte est désactivé. Contactez l'administrateur.");
        }
        Map<String, Object> jeton = keycloak.connexion(email, request.motDePasse());
        if (compte == null) {
            // mot de passe juste, mais aucun profil MediVault : on ferme la session Keycloak qu'on vient d'ouvrir
            keycloak.deconnecter(texte(jeton, "refresh_token"));
            throw new CompteInaccessibleException("Ce compte n'est pas rattaché à un profil MediVault.");
        }
        // première connexion d'un compte de démonstration : on relie le profil à son identité Keycloak
        String sub = (String) claims(texte(jeton, "access_token")).get("sub");
        if (compte.getKeycloakId() == null && sub != null) {
            compte.setKeycloakId(sub);
            compte = utilisateurRepository.save(compte);
        }
        log.info("Connexion de {} ({})", compte.getEmail(), compte.getRole());
        return session(jeton, compte);
    }

    @Override
    public SessionResponse renouveler(String refreshToken) {
        Map<String, Object> jeton = keycloak.renouveler(refreshToken);
        String email = (String) claims(texte(jeton, "access_token")).get("email");
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> new CompteInaccessibleException("Ce compte n'est pas rattaché à un profil MediVault."));
        if (!compte.isActif()) {
            keycloak.deconnecter(texte(jeton, "refresh_token"));
            throw new CompteInaccessibleException("Ce compte est désactivé. Contactez l'administrateur.");
        }
        return session(jeton, compte);
    }

    @Override
    public void deconnexion(String refreshToken) {
        keycloak.deconnecter(refreshToken);
    }

    @Override
    public UtilisateurResponse inscription(InscriptionRequest r) {
        // Un visiteur ne peut s'inscrire que comme PATIENT : le rôle est imposé ici, jamais lu dans la requête.
        UtilisateurRequest demande = new UtilisateurRequest(r.nom(), r.prenom(), r.email(), r.telephone(), r.motDePasse(),
                Role.PATIENT, null, null, null, null, null, null, null);
        return utilisateurService.creer(demande);
    }

    @Override
    public MessageResponse motDePasseOublie(String email) {
        String courriel = normaliser(email);
        utilisateurRepository.findByEmailIgnoreCase(courriel).ifPresent(compte -> {
            try {
                if (emailApi.isActif()) {
                    // API d'e-mail configurée : on envoie nous-mêmes un lien à usage unique, dans la vraie boîte mail
                    if (compte.isActif()) {
                        envoyerLienParApi(compte);
                    }
                } else {
                    // sinon : Keycloak envoie l'e-mail (visible dans Mailpit)
                    keycloak.envoyerEmailReinitialisation(identifiantKeycloak(compte), urlRetourApresReinitialisation);
                }
                log.info("E-mail de réinitialisation envoyé à {}", courriel);
            } catch (RuntimeException e) {
                // on ne révèle rien à l'appelant (même réponse dans tous les cas) : on trace seulement l'échec
                log.warn("E-mail de réinitialisation non envoyé à {} : {}", courriel, e.getMessage());
            }
        });
        return new MessageResponse("Si un compte existe avec cette adresse, un e-mail de réinitialisation vient d'être envoyé.");
    }

    @Override
    public MessageResponse reinitialiserMotDePasse(String lien, String nouveauMotDePasse) {
        JetonReinitialisation jeton = jetonRepository.findByEmpreinte(empreinte(lien == null ? "" : lien))
                .filter(j -> j.getExpiration().isAfter(Instant.now()))
                .orElseThrow(() -> new LienInvalideException("Ce lien est invalide ou a expiré : refaites une demande « Mot de passe oublié »."));
        Utilisateur compte = utilisateurRepository.findById(jeton.getUtilisateurId())
                .orElseThrow(() -> new LienInvalideException("Ce lien est invalide ou a expiré : refaites une demande « Mot de passe oublié »."));
        if (!compte.isActif()) {
            throw new CompteInaccessibleException("Ce compte est désactivé. Contactez l'administrateur.");
        }
        keycloak.changerMotDePasse(identifiantKeycloak(compte), nouveauMotDePasse);
        jetonRepository.deleteByUtilisateurId(compte.getId());    // à usage unique : le lien (et les anciens) ne servent plus
        log.info("Mot de passe réinitialisé pour {}", compte.getEmail());
        return new MessageResponse("Votre mot de passe a été modifié. Vous pouvez vous connecter.");
    }

    @Override
    public UtilisateurResponse moi(String email) {
        return UtilisateurResponse.from(utilisateurRepository.findByEmailIgnoreCase(normaliser(email))
                .orElseThrow(() -> new UtilisateurNotFoundException("Aucun profil pour " + email)));
    }

    @Override
    public MessageResponse changerMotDePasse(String email, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(normaliser(email))
                .orElseThrow(() -> new UtilisateurNotFoundException("Aucun profil pour " + email));
        try {
            Map<String, Object> jeton = keycloak.connexion(compte.getEmail(), ancienMotDePasse);   // vérifie l'ancien mot de passe
            keycloak.deconnecter(texte(jeton, "refresh_token"));
        } catch (IdentifiantsInvalidesException e) {
            throw new IdentifiantsInvalidesException("Le mot de passe actuel est incorrect.");
        }
        keycloak.changerMotDePasse(identifiantKeycloak(compte), nouveauMotDePasse);
        return new MessageResponse("Votre mot de passe a été modifié.");
    }

    // ---------------------------------------------------------------- outils

    /** Crée un lien à usage unique (seule son empreinte est gardée) et l'envoie par l'API d'e-mail. */
    private void envoyerLienParApi(Utilisateur compte) {
        byte[] octets = new byte[32];
        hasard.nextBytes(octets);
        String lien = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        jetonRepository.deleteByUtilisateurId(compte.getId());     // une seule demande valable à la fois
        jetonRepository.save(JetonReinitialisation.builder().empreinte(empreinte(lien)).utilisateurId(compte.getId())
                .expiration(Instant.now().plus(dureeLienMinutes, ChronoUnit.MINUTES)).build());
        String adresse = urlPageReinitialisation + "?token=" + lien;
        String prenom = echapper(compte.getPrenom());
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#0a1a2a\">"
                + "<h2>MediVault</h2><p>Bonjour " + prenom + ",</p>"
                + "<p>Vous avez demandé à changer votre mot de passe. Ce lien est valable " + dureeLienMinutes + " minutes et ne sert qu'une fois.</p>"
                + "<p><a href=\"" + adresse + "\" style=\"display:inline-block;padding:12px 22px;background:#0a1a2a;color:#fff;"
                + "border-radius:10px;text-decoration:none\">Choisir un nouveau mot de passe</a></p>"
                + "<p style=\"font-size:12px;color:#667\">Si vous n'êtes pas à l'origine de cette demande, ignorez ce message : votre mot de passe ne change pas.</p></div>";
        emailApi.envoyer(compte.getEmail(), compte.getPrenom() + " " + compte.getNom(), "MediVault : nouveau mot de passe", html);
    }

    private static String empreinte(String lien) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lien.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String echapper(String texte) {
        return texte == null ? "" : texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String identifiantKeycloak(Utilisateur compte) {
        String id = compte.getKeycloakId() != null ? compte.getKeycloakId() : keycloak.trouverIdParEmail(compte.getEmail());
        if (id == null) {
            throw new CompteInaccessibleException("Ce compte n'a pas d'identité Keycloak.");
        }
        return id;
    }

    private SessionResponse session(Map<String, Object> jeton, Utilisateur compte) {
        return new SessionResponse(texte(jeton, "access_token"), texte(jeton, "refresh_token"),
                nombre(jeton, "expires_in"), nombre(jeton, "refresh_expires_in"), "Bearer", UtilisateurResponse.from(compte));
    }

    private static String normaliser(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String texte(Map<String, Object> jeton, String cle) {
        Object v = jeton.get(cle);
        if (v == null) {
            throw new KeycloakIndisponibleException("Réponse Keycloak incomplète : " + cle + " manquant", null);
        }
        return v.toString();
    }

    private static long nombre(Map<String, Object> jeton, String cle) {
        return jeton.get(cle) instanceof Number n ? n.longValue() : 0L;
    }

    /** Lit le contenu (claims) d'un jeton reçu directement de Keycloak : pas besoin de revérifier sa signature. */
    private Map<String, Object> claims(String jwt) {
        try {
            byte[] contenu = Base64.getUrlDecoder().decode(jwt.split("\\.")[1]);
            return objectMapper.readValue(contenu, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new KeycloakIndisponibleException("Jeton Keycloak illisible", e);
        }
    }
}
