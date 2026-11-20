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
    private final RestClient restClient;

    public KeycloakAdminClient(@Value("${medivault.keycloak.enabled:false}") boolean enabled,
                               @Value("${medivault.keycloak.server-url}") String serverUrl,
                               @Value("${medivault.keycloak.realm}") String realm,
                               @Value("${medivault.keycloak.client-id}") String clientId,
                               @Value("${medivault.keycloak.client-secret}") String clientSecret) {
        this.enabled = enabled;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
