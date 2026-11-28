package tn.esprit.medivault.medicament.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tn.esprit.medivault.medicament.entities.*;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;
import tn.esprit.medivault.medicament.repositories.TentativeRefuseeRepository;
import tn.esprit.medivault.medicament.repositories.VenteSensibleRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

// Données de démonstration, créées UNE SEULE FOIS : seulement si la base est vide.
// Comme la base H2 est un fichier, elles sont conservées d'un redémarrage à l'autre.
// Pour ne pas les charger : medivault.demo-data=false (application.properties).
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "medivault.demo-data", havingValue = "true", matchIfMissing = true)
public class DonneesDemo implements ApplicationRunner {

    private final MedicamentSensibleRepository medicaments;
    private final AttributionRepository attributions;
    private final VenteSensibleRepository ventes;
    private final TentativeRefuseeRepository refus;
    private final Clock clock;

    // Adresse du user-service (ex. http://localhost:8081) : si elle est donnée, les quotas de démonstration sont créés
    // pour les VRAIES pharmacies de ce service. Sinon (ou s'il ne répond pas), pharmacies d'exemple.
    @Value("${medivault.demo.user-service-url:}")
    private String adresseUserService;

    private List<String> pharmacies() {
        if (!adresseUserService.isBlank()) {
            try {
                List<Map<String, Object>> comptes = RestClient.create().get()
                        .uri(adresseUserService + "/api/professionnels?type=PHARMACIEN").retrieve()
                        .body(new ParameterizedTypeReference<>() { });
                if (comptes != null && !comptes.isEmpty()) {
                    // ordre stable (l'API n'en garantit aucun) : la Pharmacie Centrale d'abord, puis par identifiant
                    return comptes.stream().map(c -> String.valueOf(c.get("id")))
                            .sorted((a, b) -> a.equals("pharmacie-centrale") ? -1 : b.equals("pharmacie-centrale") ? 1 : a.compareTo(b)).toList();
                }
            } catch (Exception e) {
                log.warn("Pharmacies du user-service introuvables ({}) : pharmacies d'exemple", e.toString());
            }
        }
        return List.of("pharmacie-centrale", "2", "3", "4", "5", "6", "7", "8");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (medicaments.count() > 0) {
            return;
        }
        List<MedicamentSensible> liste = medicaments.saveAll(List.of(
                med("Subutex 8 mg", "Buprénorphine", Categorie.STUPEFIANT,
                        "Traitement de substitution des dépendances aux opioïdes : réduit le besoin et les symptômes de manque.",
                        "Ne pas associer à l'alcool ni aux benzodiazépines. Comprimé à laisser fondre sous la langue. Risque de somnolence."),
                med("Méthadone 40 mg", "Méthadone", Categorie.STUPEFIANT,
                        "Traitement de substitution aux opioïdes et antalgique de palier 3.",
                        "Dose strictement individuelle. Risque de surdosage en début de traitement. Éviter la conduite."),
                med("Skenan 30 mg", "Morphine", Categorie.STUPEFIANT,
                        "Antalgique opioïde fort pour les douleurs intenses, notamment cancéreuses.",
                        "Constipation et somnolence fréquentes. Ne jamais écraser la gélule. Risque de dépendance."),
                med("Ritaline 10 mg", "Méthylphénidate", Categorie.PSYCHOTROPE,
                        "Stimulant du système nerveux central utilisé dans le trouble du déficit de l'attention.",
                        "À prendre le matin. Peut perturber le sommeil et l'appétit. Surveillance de la tension."),
                med("Rivotril 2 mg", "Clonazépam", Categorie.PSYCHOTROPE,
                        "Antiépileptique et anxiolytique de la famille des benzodiazépines.",
                        "Traitement court. Risque de dépendance et de somnolence. Ne pas arrêter brutalement."),
                med("Xanax 0,5 mg", "Alprazolam", Categorie.PSYCHOTROPE,
                        "Anxiolytique à action rapide, pour les états d'anxiété sévères.",
                        "Éviter l'alcool. Ne pas conduire. Durée de traitement la plus courte possible."),
                med("Misoprostol 200 µg", "Misoprostol", Categorie.ABORTIF,
                        "Provoque des contractions de l'utérus : utilisé sous contrôle médical pour interrompre une grossesse.",
                        "Strictement contre-indiqué en dehors d'une prescription encadrée. Saignements et douleurs à surveiller.")));

        YearMonth mois = YearMonth.now(clock);
        int compteur = 0;
        // les pharmacies (celles du user-service si possible) ; chaque médicament est attribué à 5 d'entre elles
        List<String> idsPharmacies = pharmacies();
        for (MedicamentSensible m : liste) {
            for (int p = 1; p <= idsPharmacies.size(); p++) {
                if ((p + m.getId().intValue()) % 8 >= 5) {
                    continue;
                }
                int quota = 40 + ((p * 7 + m.getId().intValue() * 13) % 8) * 10;
                Attribution a = attributions.save(Attribution.builder()
                        .pharmacieId(idsPharmacies.get(p - 1)).medicament(m).mois(mois).quantiteAttribuee(quota).build());
                // quelques ventes passées, cohérentes avec la quantité vendue du quota
                int vendu = 0;
                for (int v = 0; v < 3; v++) {
                    int quantite = 2 + (compteur * 3 + v) % 7;
                    if (vendu + quantite > quota * 8 / 10) {
                        break;
                    }
                    compteur++;
                    ventes.save(VenteSensible.builder()
                            .patientId("patient-" + (1 + compteur % 9)).ordonnanceId(2000L + compteur)
                            .ordonnanceCode("ORD-DEMO-" + (1000 + compteur)).quantite(quantite)
                            .date(LocalDateTime.now(clock).minusDays(compteur % 12)).attribution(a).build());
                    vendu += quantite;
                }
                // un quota qui approche de l'épuisement (86 %) : on voit l'alerte à 80 % et la prévision de date
                if (p == 2 && m.getId() == 2L && vendu < quota * 86 / 100) {
                    int extra = quota * 86 / 100 - vendu;
                    compteur++;
                    ventes.save(VenteSensible.builder().patientId("patient-" + (1 + compteur % 9)).ordonnanceId(2000L + compteur)
                            .ordonnanceCode("ORD-DEMO-" + (1000 + compteur)).quantite(extra)
                            .date(LocalDateTime.now(clock).minusDays(1)).attribution(a).build());
                    vendu += extra;
                }
                a.ajouterVente(vendu);
                // un exemple d'avertissement et un exemple de suspension
                // chaque avertissement a son refus dans l'historique (comme en vrai)
                if (p == 1 && m.getId() == 1L) {
                    a.ajouterAvertissement();
                    refusDemo(a, MotifRefus.QUOTA_EPUISE, 12, 3, "Quota du mois insuffisant.");
                    refusDemo(a, MotifRefus.AUCUN_QUOTA, 4, 5, "Aucun quota attribué pour ce mois.");   // pas d'avertissement pour ce motif
                }
                if (p == 6 && m.getId() == 6L) {
                    a.ajouterAvertissement();
                    a.ajouterAvertissement();
                    refusDemo(a, MotifRefus.ORDONNANCE_NON_JUSTIFIEE, 9, 6, "Cette ordonnance ne contient pas ce médicament.");
                    refusDemo(a, MotifRefus.QUOTA_EPUISE, 14, 1, "Quota du mois insuffisant.");
                }
                attributions.save(a);
            }
        }
        log.info("Données de démonstration créées : {} médicaments sensibles, {} quotas, {} ventes",
                medicaments.count(), attributions.count(), ventes.count());
    }

    // Un refus de démonstration, daté de quelques jours en arrière
    private void refusDemo(Attribution a, MotifRefus motif, int quantite, int joursAvant, String detail) {
        refus.save(TentativeRefusee.builder().pharmacieId(a.getPharmacieId()).medicamentId(a.getMedicament().getId())
                .medicamentNom(a.getMedicament().getNomCommercial()).ordonnanceCode("ORD-DEMO-REFUS").quantite(quantite)
                .date(LocalDateTime.now(clock).minusDays(joursAvant)).motif(motif).detail(detail).build());
    }

    private MedicamentSensible med(String nom, String dci, Categorie categorie, String description, String precautions) {
        return MedicamentSensible.builder().nomCommercial(nom).dci(dci).categorie(categorie)
                .description(description).precautions(precautions).build();
    }
}
