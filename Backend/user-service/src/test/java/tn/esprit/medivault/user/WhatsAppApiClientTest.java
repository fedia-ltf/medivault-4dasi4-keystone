package tn.esprit.medivault.user;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.medivault.user.clients.WhatsAppApiClient;
import tn.esprit.medivault.user.exceptions.EmailIndisponibleException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Le client WhatsApp (Twilio) face à un faux serveur local : on vérifie l'appel réellement envoyé.
class WhatsAppApiClientTest {

    private HttpServer serveur;
    private int statut = 201;
    private final AtomicReference<String> chemin = new AtomicReference<>();
    private final AtomicReference<String> autorisation = new AtomicReference<>();
    private final AtomicReference<String> corps = new AtomicReference<>();

    @BeforeEach
    void demarrer() throws Exception {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", ex -> {
            chemin.set(ex.getRequestMethod() + " " + ex.getRequestURI().getPath());
            autorisation.set(ex.getRequestHeaders().getFirst("Authorization"));
            corps.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reponse = "{}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(statut, reponse.length);
            ex.getResponseBody().write(reponse);
            ex.close();
        });
        serveur.start();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    private WhatsAppApiClient client(String sid, String jeton) {
        return new WhatsAppApiClient(sid, jeton, "+14155238886", "http://127.0.0.1:" + serveur.getAddress().getPort());
    }

    @Test
    void inactifSansIdentifiants() {
        assertThat(client("", "jeton").isActif()).isFalse();
        assertThat(client("ACxxx", "").isActif()).isFalse();
        assertThat(client("ACxxx", "jeton").isActif()).isTrue();
    }

    @Test
    void envoyer_posteLeMessageAvecLAuthentificationBasic() {
        client("ACtest123", "secret").envoyer("+21622123456", "Bonjour & merci");

        assertThat(chemin.get()).isEqualTo("POST /2010-04-01/Accounts/ACtest123/Messages.json");
        assertThat(autorisation.get()).isEqualTo("Basic " + Base64.getEncoder().encodeToString("ACtest123:secret".getBytes(StandardCharsets.UTF_8)));
        assertThat(corps.get()).contains("From=whatsapp%3A%2B14155238886").contains("To=whatsapp%3A%2B21622123456")
                .contains("Body=Bonjour+%26+merci");
    }

    @Test
    void envoyer_refus_leveUneException() {
        statut = 401;
        assertThatThrownBy(() -> client("ACtest123", "faux").envoyer("+21622123456", "x")).isInstanceOf(EmailIndisponibleException.class);
    }
}
