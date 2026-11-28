package tn.esprit.medivault.user.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.exceptions.EmailDejaUtiliseException;
import tn.esprit.medivault.user.exceptions.CompteInaccessibleException;
import tn.esprit.medivault.user.exceptions.IdentifiantsInvalidesException;
import tn.esprit.medivault.user.exceptions.KeycloakIndisponibleException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Appel synchrone vers l'API Admin de Keycloak : création du compte, attribution du rôle,
 * activation / désactivation. Le client "user-service" du realm "medivault" doit avoir
 * un service account avec les rôles realm-management "manage-users" et "view-realm".
 * Tant que medivault.keycloak.enabled=false (Keycloak pas encore prêt), rien n'est appelé.
 */
@Component
@Slf4j
public class KeycloakAdminClient {

    private final boolean enabled;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String frontendClientId;
    private final RestClient restClient;

    public KeycloakAdminClient(@Value("${medivault.keycloak.enabled:false}") boolean enabled,
                               @Value("${medivault.keycloak.server-url}") String serverUrl,
                               @Value("${medivault.keycloak.realm}") String realm,
                               @Value("${medivault.keycloak.client-id}") String clientId,
                               @Value("${medivault.keycloak.client-secret}") String clientSecret,
                               @Value("${medivault.keycloak.frontend-client-id:medivault-frontend}") String frontendClientId) {
        this.enabled = enabled;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.frontendClientId = frontendClientId;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        this.restClient = RestClient.builder().baseUrl(serverUrl).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Crée le compte Keycloak avec son rôle et renvoie son identifiant (null si Keycloak est désactivé). */
    public String creerCompte(String email, String prenom, String nom, String motDePasse, Role role) {
        if (!enabled) {
            log.info("Keycloak désactivé : compte {} enregistré sans identité Keycloak", email);
            return null;
        }
        try {
            String token = token();
            Map<String, Object> user = Map.of(
                    "username", email,
                    "email", email,
                    "firstName", prenom,
                    "lastName", nom,
                    "enabled", true,
                    "emailVerified", true,
                    "credentials", List.of(Map.of("type", "password", "value", motDePasse, "temporary", false)));

            ResponseEntity<Void> reponse = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();

            URI location = reponse.getHeaders().getLocation();
            if (location == null) {
                throw new KeycloakIndisponibleException("Keycloak n'a pas renvoyé l'identifiant du compte", null);
            }
            String path = location.getPath();
            String keycloakId = path.substring(path.lastIndexOf('/') + 1);

            attribuerRole(token, keycloakId, role);
            return keycloakId;
        } catch (HttpClientErrorException.Conflict e) {
            throw new EmailDejaUtiliseException(email);
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("Keycloak indisponible : compte non créé, réessayer plus tard", e);
        }
    }

    public void definirActif(String keycloakId, boolean actif) {
        if (!enabled || keycloakId == null) {
            return;
        }
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", actif))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("Keycloak indisponible : statut non modifié, réessayer plus tard", e);
        }
    }

    /** Annule la création si l'enregistrement MongoDB échoue (pas de compte Keycloak orphelin). */
    public void supprimerCompte(String keycloakId) {
        if (!enabled || keycloakId == null) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header("Authorization", "Bearer " + token())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Compte Keycloak {} non supprimé : {}", keycloakId, e.getMessage());
        }
    }

    private void attribuerRole(String token, String keycloakId, Role role) {
        Map<String, Object> representation = restClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", realm, role.name())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, keycloakId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(representation))
                .retrieve()
                .toBodilessEntity();
    }

    // ------------------------------------------------------------------ connexion (login) et jetons

    /** Vérifie l'email et le mot de passe auprès de Keycloak et renvoie ses jetons (access_token, refresh_token…). */
    public Map<String, Object> connexion(String email, String motDePasse) {
        exigerActif();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", frontendClientId);
        form.add("username", email);
        form.add("password", motDePasse);
        form.add("scope", "openid");
        return demanderJeton(form, "Email ou mot de passe incorrect.");
    }

    /** Échange un refresh_token contre de nouveaux jetons. */
    public Map<String, Object> renouveler(String refreshToken) {
        exigerActif();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", frontendClientId);
        form.add("refresh_token", refreshToken);
        return demanderJeton(form, "Votre session a expiré : reconnectez-vous.");
    }

    /** Ferme la session Keycloak (le refresh_token ne sert plus). Sans effet si Keycloak est désactivé ou injoignable. */
    public void deconnecter(String refreshToken) {
        if (!enabled || refreshToken == null) {
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", frontendClientId);
        form.add("refresh_token", refreshToken);
        try {
            restClient.post().uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Déconnexion Keycloak non confirmée : {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ mot de passe

    /** L'identifiant Keycloak d'un compte à partir de son email (null s'il n'existe pas). */
    public String trouverIdParEmail(String email) {
        exigerActif();
        try {
            List<Map<String, Object>> trouves = restClient.get()
                    .uri(b -> b.path("/admin/realms/{realm}/users").queryParam("email", email).queryParam("exact", true).build(realm))
                    .header("Authorization", "Bearer " + token())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return trouves == null || trouves.isEmpty() ? null : trouves.get(0).get("id").toString();
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("Keycloak indisponible : réessayer plus tard", e);
        }
    }

    /** « Mot de passe oublié » : Keycloak envoie lui-même l'e-mail avec le lien pour choisir un nouveau mot de passe (30 minutes). */
    public void envoyerEmailReinitialisation(String keycloakId, String urlRetour) {
        exigerActif();
        try {
            restClient.put()
                    .uri(b -> b.path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                            .queryParam("client_id", frontendClientId).queryParam("redirect_uri", urlRetour)
                            .queryParam("lifespan", 1800).build(realm, keycloakId))
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("L'e-mail n'a pas pu être envoyé : " + e.getMessage(), e);
        }
    }

    /** Définit un nouveau mot de passe (non temporaire). */
    public void changerMotDePasse(String keycloakId, String nouveauMotDePasse) {
        exigerActif();
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, keycloakId)
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", nouveauMotDePasse, "temporary", false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IdentifiantsInvalidesException("Mot de passe refusé : 8 caractères minimum, avec au moins un chiffre.");
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("Keycloak indisponible : mot de passe non modifié", e);
        }
    }

    private void exigerActif() {
        if (!enabled) {
            throw new KeycloakIndisponibleException(
                    "Connexion indisponible : Keycloak est désactivé (lancer avec le profil « keycloak »).", null);
        }
    }

    private Map<String, Object> demanderJeton(MultiValueMap<String, String> form, String messageRefus) {
        try {
            Map<String, Object> reponse = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (reponse == null || reponse.get("access_token") == null) {
                throw new KeycloakIndisponibleException("Jeton Keycloak non obtenu", null);
            }
            return reponse;
        } catch (HttpClientErrorException e) {
            String corps = e.getResponseBodyAsString();
            if (corps.contains("Account disabled")) {
                throw new CompteInaccessibleException("Ce compte est désactivé. Contactez l'administrateur.");
            }
            if (corps.contains("temporarily disabled")) {
                throw new IdentifiantsInvalidesException("Trop de tentatives : compte bloqué quelques minutes.");
            }
            throw new IdentifiantsInvalidesException(messageRefus);
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException("Keycloak indisponible : réessayer plus tard", e);
        }
    }

    private String token() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> reponse = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        if (reponse == null || reponse.get("access_token") == null) {
            throw new KeycloakIndisponibleException("Jeton Keycloak non obtenu", null);
        }
        return reponse.get("access_token").toString();
    }
}
