package tn.esprit.medivault.user.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.medivault.user.exceptions.EmailIndisponibleException;

/**
 * Envoi d'un message WhatsApp par l'API de Twilio (« mot de passe oublié » par WhatsApp).
 * Mode test gratuit : le « sandbox » Twilio. Le téléphone qui doit recevoir les messages envoie une fois « join <mot-du-sandbox> »
 * au numéro du sandbox (+1 415 523 8886) ; la session reste ouverte 72 h, puis on renvoie « join ».
 * Les identifiants (Account SID + Auth Token) restent dans secrets.properties (ignoré par Git), jamais dans le code.
 * Sans identifiants, le client est inactif et la demande retombe sur l'e-mail.
 */
@Component
@Slf4j
public class WhatsAppApiClient {

    private final String compteSid;
    private final String jeton;
    private final String expediteur;
    private final RestClient restClient;

    public WhatsAppApiClient(@Value("${medivault.whatsapp.account-sid:}") String compteSid,
                             @Value("${medivault.whatsapp.auth-token:}") String jeton,
                             @Value("${medivault.whatsapp.from:+14155238886}") String expediteur,
                             @Value("${medivault.whatsapp.api-url:https://api.twilio.com}") String urlApi) {
        this.compteSid = compteSid == null ? "" : compteSid.trim();
        this.jeton = jeton == null ? "" : jeton.trim();
        this.expediteur = expediteur == null ? "" : expediteur.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder().baseUrl(urlApi).requestFactory(factory).build();
    }

    public boolean isActif() {
        return !compteSid.isBlank() && !jeton.isBlank() && !expediteur.isBlank();
    }

    /** Envoie un message texte au numéro (format international : +216XXXXXXXX). Lève EmailIndisponibleException en cas de refus. */
    public void envoyer(String numero, String texte) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", "whatsapp:" + expediteur);
        form.add("To", "whatsapp:" + numero);
        form.add("Body", texte);
        try {
            restClient.post().uri("/2010-04-01/Accounts/{sid}/Messages.json", compteSid)
                    .headers(h -> h.setBasicAuth(compteSid, jeton))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new EmailIndisponibleException("WhatsApp (Twilio) a refusé ou n'a pas répondu : " + e.getMessage(), e);
        }
    }
}
