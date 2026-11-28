package tn.esprit.medivault.soindomicile.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tn.esprit.medivault.soindomicile.entities.Visite;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Prévient l'infirmier sur WhatsApp quand le pharmacien l'affecte à une visite.
 *
 * Deux modes :
 *  - automatique : si une clé CallMeBot est configurée, le message part tout seul
 *    (https://api.callmebot.com/whatsapp.php). La clé est liée au numéro qui l'a demandée au bot ;
 *  - manuel      : sinon, on fournit un lien wa.me qui ouvre WhatsApp avec le message déjà écrit.
 * Un échec d'envoi est seulement journalisé : il n'annule jamais l'affectation.
 */
@Component
@Slf4j
public class WhatsAppNotifier {

    private static final String API_CALLMEBOT = "https://api.callmebot.com/whatsapp.php";
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final String apiKey;
    private final RestClient restClient;

    public WhatsAppNotifier(@Value("${medivault.whatsapp.callmebot.apikey:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        SimpleClientHttpRequestFactory delais = new SimpleClientHttpRequestFactory();
        delais.setConnectTimeout(3000);   // l'affectation ne doit pas rester bloquée si le service ne répond pas
        delais.setReadTimeout(8000);
        this.restClient = RestClient.builder().requestFactory(delais).build();
    }

    /** Vrai si l'envoi automatique (CallMeBot) est configuré. */
    public boolean estConfigure() {
        return !apiKey.isEmpty();
    }

    /** Texte envoyé à l'infirmier : infos du patient et du soin. */
    public String message(Visite v) {
        StringBuilder m = new StringBuilder()
                .append("*MediVault - Nouveau soin à domicile*\n")
                .append("Bonjour ").append(v.getInfirmierNom()).append(",\n")
                .append("Vous êtes affecté(e) au soin suivant :\n")
                .append("- Patient : ").append(v.getPatientId()).append('\n')
                .append("- Soin : ").append(soinEtDose(v)).append('\n')
                .append("- Date : ").append(v.getDate().format(FORMAT_DATE)).append('\n')
                .append("- Adresse : ").append(v.getAdresse()).append('\n');
        if (v.getLatitude() != null && v.getLongitude() != null) {
            m.append("- Carte : https://www.google.com/maps?q=")
                    .append(v.getLatitude()).append(',').append(v.getLongitude()).append('\n');
        }
        if (v.getOrdonnanceId() != null) m.append("- Ordonnance n° ").append(v.getOrdonnanceId()).append('\n');
        m.append("- Pharmacie : ").append(v.getPharmacieId()).append('\n')
                .append("Merci de vous présenter à l'heure.");
        return m.toString();
    }

    /** Lien wa.me qui ouvre WhatsApp vers l'infirmier avec le message déjà écrit. */
    public String lien(Visite v) {
        return "https://wa.me/" + numeroInternational(v.getInfirmierTelephone()) + "?text=" + encoder(message(v)).replace("+", "%20");
    }

    /** Envoi automatique par CallMeBot. Renvoie vrai si le message est accepté. */
    public boolean envoyer(Visite v) {
        String numero = numeroInternational(v.getInfirmierTelephone());
        if (!estConfigure() || numero.isEmpty()) {
            log.info("WhatsApp : envoi automatique désactivé ou numéro absent, le pharmacien utilisera le lien wa.me (visite {})", v.getId());
            return false;
        }
        try {
            URI url = URI.create(API_CALLMEBOT
                    + "?phone=" + encoder("+" + numero)
                    + "&apikey=" + encoder(apiKey)
                    + "&text=" + encoder(message(v)));
            String reponse = restClient.get().uri(url).retrieve().body(String.class);
            String texte = reponse == null ? "" : reponse.toLowerCase();
            if (texte.contains("error") || texte.contains("invalid")) {
                log.warn("WhatsApp refusé par CallMeBot pour la visite {} : {}", v.getId(), resume(reponse));
                return false;
            }
            log.info("WhatsApp envoyé à {} (+{}) pour la visite {}", v.getInfirmierNom(), numero, v.getId());
            return true;
        } catch (Exception e) {
            log.warn("Envoi WhatsApp impossible pour la visite {} : {}", v.getId(), e.getMessage());
            return false;
        }
    }

    /** "+216 22 145 870", "0021622145870" ou "22145870" -> "21622145870" (chiffres seulement, avec indicatif). */
    static String numeroInternational(String telephone) {
        if (telephone == null) return "";
        String chiffres = telephone.replaceAll("[^0-9]", "");
        if (chiffres.startsWith("00")) chiffres = chiffres.substring(2);
        if (chiffres.length() == 8) chiffres = "216" + chiffres;    // numéro tunisien saisi sans indicatif
        return chiffres;
    }

    private static String soinEtDose(Visite v) {
        String soin = v.getTypeSoin() == null ? "" : switch (v.getTypeSoin()) {
            case INJECTION -> "Injection";
            case MESURE_TENSION -> "Mesure de tension";
            case GLYCEMIE -> "Glycémie";
            case PRISE_DE_SANG -> "Prise de sang";
        };
        return v.getDose() != null && !v.getDose().isBlank() ? soin + " (" + v.getDose() + ")" : soin;
    }

    private static String encoder(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** La réponse de CallMeBot est une page HTML : on n'en garde que le texte, en court. */
    private static String resume(String html) {
        String texte = html == null ? "" : html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return texte.length() > 200 ? texte.substring(0, 200) + "…" : texte;
    }
}
