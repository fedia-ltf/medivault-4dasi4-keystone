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
 * Envoi d'e-mails par l'API HTTP d'un service d'e-mail : pas de serveur SMTP à régler, un simple appel POST avec une clé.
 * Deux services sont gérés (medivault.mail.provider) :
 * <ul>
 *   <li><b>brevo</b> (https://developers.brevo.com) : l'expéditeur doit être validé, et le compte activé par Brevo ;</li>
 *   <li><b>resend</b> (https://resend.com) : actif tout de suite. Sans domaine à soi, l'expéditeur est onboarding@resend.dev
 *       et le message n'est livré qu'à l'adresse du compte Resend (suffisant pour tester avec son propre e-mail).</li>
 * </ul>
 * La clé n'est JAMAIS écrite dans un fichier du projet : secrets.properties (ignoré par Git) ou variable d'environnement.
 * Sans clé, le client est inactif et « mot de passe oublié » reste géré par Keycloak (e-mail dans Mailpit).
 */
@Component
@Slf4j
public class EmailApiClient {

    private static final String EXPEDITEUR_RESEND_PAR_DEFAUT = "onboarding@resend.dev";

    private final boolean resend;
    private final String cleApi;
    private final String expediteurEmail;
    private final String expediteurNom;
    private final RestClient restClient;

    public EmailApiClient(@Value("${medivault.mail.api-key:}") String cleApi,
                          @Value("${medivault.mail.expediteur-email:}") String expediteurEmail,
                          @Value("${medivault.mail.expediteur-nom:MediVault}") String expediteurNom,
                          @Value("${medivault.mail.api-url:}") String urlApi,
                          @Value("${medivault.mail.provider:brevo}") String fournisseur) {
        this.resend = "resend".equalsIgnoreCase(fournisseur == null ? "" : fournisseur.trim());
        this.cleApi = cleApi == null ? "" : cleApi.trim();
        String expediteur = expediteurEmail == null ? "" : expediteurEmail.trim();
        this.expediteurEmail = expediteur.isBlank() && resend ? EXPEDITEUR_RESEND_PAR_DEFAUT : expediteur;
        this.expediteurNom = expediteurNom;
        String url = urlApi == null || urlApi.isBlank() ? (resend ? "https://api.resend.com" : "https://api.brevo.com/v3") : urlApi.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    /** Vrai si une clé d'API et une adresse d'expéditeur sont configurées. */
    public boolean isActif() {
        return !cleApi.isBlank() && !expediteurEmail.isBlank();
    }

    /** Envoie un e-mail HTML. Lève EmailIndisponibleException si le service refuse ou ne répond pas. */
    public void envoyer(String destinataireEmail, String destinataireNom, String sujet, String html) {
        try {
            if (resend) {
                restClient.post().uri("/emails")
                        .header("Authorization", "Bearer " + cleApi)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("from", expediteurNom + " <" + expediteurEmail + ">", "to", List.of(destinataireEmail),
                                "subject", sujet, "html", html))
                        .retrieve().toBodilessEntity();
            } else {
                restClient.post().uri("/smtp/email")
                        .header("api-key", cleApi)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("sender", Map.of("name", expediteurNom, "email", expediteurEmail),
                                "to", List.of(Map.of("email", destinataireEmail, "name", destinataireNom)),
                                "subject", sujet, "htmlContent", html))
                        .retrieve().toBodilessEntity();
            }
        } catch (RestClientException e) {
            throw new EmailIndisponibleException("Le service d'e-mail a refusé ou n'a pas répondu : " + e.getMessage(), e);
        }
    }
}
