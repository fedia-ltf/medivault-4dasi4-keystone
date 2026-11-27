package tn.esprit.medivault.user.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.medivault.user.exceptions.EmailIndisponibleException;

import java.util.List;
import java.util.Map;

/**
 * Envoi d'e-mails par l'API HTTP d'un service d'e-mail (Brevo : https://developers.brevo.com, offre gratuite).
 * Pas de serveur SMTP à régler : un simple appel POST avec une clé d'API.
 * La clé n'est JAMAIS écrite dans un fichier du projet : variable d'environnement BREVO_API_KEY.
 * Sans clé, le client est inactif et « mot de passe oublié » reste géré par Keycloak (e-mail dans Mailpit).
 */
@Component
@Slf4j
public class EmailApiClient {

    private final String cleApi;
    private final String expediteurEmail;
    private final String expediteurNom;
    private final RestClient restClient;

    public EmailApiClient(@Value("${medivault.mail.api-key:}") String cleApi,
                          @Value("${medivault.mail.expediteur-email:}") String expediteurEmail,
                          @Value("${medivault.mail.expediteur-nom:MediVault}") String expediteurNom,
                          @Value("${medivault.mail.api-url:https://api.brevo.com/v3}") String urlApi) {
        this.cleApi = cleApi == null ? "" : cleApi.trim();
        this.expediteurEmail = expediteurEmail == null ? "" : expediteurEmail.trim();
        this.expediteurNom = expediteurNom;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder().baseUrl(urlApi).requestFactory(factory).build();
    }

    /** Vrai si une clé d'API et une adresse d'expéditeur sont configurées. */
    public boolean isActif() {
        return !cleApi.isBlank() && !expediteurEmail.isBlank();
    }

    /** Envoie un e-mail HTML. Lève EmailIndisponibleException si le service refuse ou ne répond pas. */
    public void envoyer(String destinataireEmail, String destinataireNom, String sujet, String html) {
        Map<String, Object> message = Map.of(
                "sender", Map.of("name", expediteurNom, "email", expediteurEmail),
                "to", List.of(Map.of("email", destinataireEmail, "name", destinataireNom)),
                "subject", sujet,
                "htmlContent", html);
        try {
            restClient.post().uri("/smtp/email")
                    .header("api-key", cleApi)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new EmailIndisponibleException("Le service d'e-mail a refusé ou n'a pas répondu : " + e.getMessage(), e);
        }
    }
}
