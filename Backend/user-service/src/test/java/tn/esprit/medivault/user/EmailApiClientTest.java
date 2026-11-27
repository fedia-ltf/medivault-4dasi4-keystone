package tn.esprit.medivault.user;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.medivault.user.clients.EmailApiClient;
import tn.esprit.medivault.user.exceptions.EmailIndisponibleException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Le client d'e-mail face à un faux serveur d'API (démarré sur un port libre) : on vérifie ce qui est réellement envoyé.
class EmailApiClientTest {

    private HttpServer serveur;
    private int statut = 201;
    private final AtomicReference<String> chemin = new AtomicReference<>();
    private final AtomicReference<String> cleRecue = new AtomicReference<>();
    private final AtomicReference<String> corps = new AtomicReference<>();

    @BeforeEach
    void demarrer() throws Exception {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", ex -> {
            chemin.set(ex.getRequestMethod() + " " + ex.getRequestURI().getPath());
            cleRecue.set(ex.getRequestHeaders().getFirst("api-key"));
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

    private EmailApiClient client(String cle, String expediteur) {
        return new EmailApiClient(cle, expediteur, "MediVault", "http://127.0.0.1:" + serveur.getAddress().getPort() + "/v3");
    }

    @Test
    void inactifSansCleOuSansExpediteur() {
        assertThat(client("", "moi@gmail.com").isActif()).isFalse();
        assertThat(client("cle", "").isActif()).isFalse();
        assertThat(client("cle", "moi@gmail.com").isActif()).isTrue();
    }

    @Test
    void envoyer_posteLeMessageAvecLaCleDApi() {
        client("ma-cle", "moi@gmail.com").envoyer("ahmed@test.tn", "Ahmed Ben Ali", "Sujet", "<p>Bonjour</p>");

        assertThat(chemin.get()).isEqualTo("POST /v3/smtp/email");
        assertThat(cleRecue.get()).isEqualTo("ma-cle");
        assertThat(corps.get()).contains("\"email\":\"moi@gmail.com\"").contains("\"email\":\"ahmed@test.tn\"")
                .contains("\"subject\":\"Sujet\"").contains("htmlContent");
    }

    @Test
    void envoyer_refusParLeService_leveUneException() {
        statut = 401;
        assertThatThrownBy(() -> client("mauvaise", "moi@gmail.com").envoyer("a@b.tn", "A", "S", "<p>x</p>"))
                .isInstanceOf(EmailIndisponibleException.class);
    }
}
