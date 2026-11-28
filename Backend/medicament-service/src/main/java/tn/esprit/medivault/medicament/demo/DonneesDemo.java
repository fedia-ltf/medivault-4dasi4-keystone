package tn.esprit.medivault.medicament.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.clients.UtilisateurClient;
import tn.esprit.medivault.medicament.dto.ExterneDtos;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

// Données de départ, créées une seule fois quand la base est vide :
//   1. le catalogue des médicaments sensibles (données de référence) ;
//   2. les quotas du mois pour les VRAIES pharmacies du user-service (lues par OpenFeign, via Eureka).
// Aucune vente, aucun refus ni ordonnance inventés : ils viennent uniquement des vraies actions des pharmacies.
// Le user-service peut démarrer après ce service : on réessaie toutes les 10 secondes pendant 5 minutes.
// Pour ne rien créer : medivault.demo-data=false (application.properties).
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "medivault.demo-data", havingValue = "true", matchIfMissing = true)
public class DonneesDemo {

    private static final int ESSAIS = 30;
    private static final long PAUSE_MS = 10_000;

    private final MedicamentSensibleRepository medicaments;
    private final AttributionRepository attributions;
    private final UtilisateurClient utilisateurs;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    public void demarrer() {
        List<MedicamentSensible> catalogue = creerCatalogue();
        if (attributions.count() > 0) {
            return;
        }
        Thread quotas = new Thread(() -> attribuerQuotas(catalogue), "quotas-de-depart");
        quotas.setDaemon(true);
        quotas.start();
    }

    private List<MedicamentSensible> creerCatalogue() {
        if (medicaments.count() > 0) {
            return medicaments.findAll();
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
        log.info("Catalogue créé : {} médicaments sensibles", liste.size());
        return liste;
    }

    // Les quotas du mois : chaque médicament est attribué à une partie des vraies pharmacies
    private void attribuerQuotas(List<MedicamentSensible> catalogue) {
        List<String> pharmacies = pharmaciesDuUserService();
        if (pharmacies.isEmpty()) {
            log.warn("user-service injoignable : aucun quota de départ. L'administrateur peut les attribuer depuis l'application.");
            return;
        }
        if (attributions.count() > 0) {
            return;
        }
        YearMonth mois = YearMonth.now(clock);
        int total = 0;
        for (MedicamentSensible m : catalogue) {
            for (int p = 1; p <= pharmacies.size(); p++) {
                if ((p + m.getId().intValue()) % 8 >= 5) {
                    continue;
                }
                int quota = 40 + ((p * 7 + m.getId().intValue() * 13) % 8) * 10;
                attributions.save(Attribution.builder()
                        .pharmacieId(pharmacies.get(p - 1)).medicament(m).mois(mois).quantiteAttribuee(quota).build());
                total++;
            }
        }
        log.info("Quotas de {} créés : {} quotas pour {} pharmacies du user-service", mois, total, pharmacies.size());
    }

    // Appel synchrone OpenFeign (user-service, trouvé par Eureka) ; réessaie tant que le service n'est pas prêt
    private List<String> pharmaciesDuUserService() {
        for (int essai = 1; essai <= ESSAIS; essai++) {
            try {
                List<String> ids = utilisateurs.professionnels("PHARMACIEN").stream()
                        .filter(ExterneDtos.Utilisateur::actif)
                        .map(ExterneDtos.Utilisateur::id)
                        // ordre stable : la Pharmacie Centrale d'abord, puis par identifiant
                        .sorted((a, b) -> a.equals("pharmacie-centrale") ? -1 : b.equals("pharmacie-centrale") ? 1 : a.compareTo(b))
                        .toList();
                if (!ids.isEmpty()) {
                    return ids;
                }
            } catch (RuntimeException e) {
                log.info("Quotas de départ : user-service pas encore prêt (essai {}/{})", essai, ESSAIS);
            }
            try {
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
        return List.of();
    }

    private MedicamentSensible med(String nom, String dci, Categorie categorie, String description, String precautions) {
        return MedicamentSensible.builder().nomCommercial(nom).dci(dci).categorie(categorie)
                .description(description).precautions(precautions).build();
    }
}
