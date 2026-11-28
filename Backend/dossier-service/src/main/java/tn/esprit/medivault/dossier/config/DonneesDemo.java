package tn.esprit.medivault.dossier.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.dossier.repositories.DossierRepository;
import tn.esprit.medivault.dossier.services.DossierService;

import java.util.List;

/**
 * Données de démonstration alignées sur user-service et analyse-service :
 * le dossier de patient-ahmed existe dès le démarrage, avec un accès ACTIF pour medecin-karim
 * et pour labo-biolab (l'analyse de démo n°1 est EN_COURS chez ce laboratoire).
 * Créées une seule fois : un accès retiré pendant la démo n'est pas réactivé au redémarrage.
 * Désactivable avec medivault.seed.enabled=false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "medivault.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DonneesDemo implements CommandLineRunner {

    private static final String PATIENT = "patient-ahmed";
    private static final List<String> PROFESSIONNELS = List.of("medecin-karim", "labo-biolab");

    private final DossierRepository dossierRepository;
    private final DossierService dossierService;

    @Override
    public void run(String... args) {
        if (dossierRepository.findByPatientId(PATIENT).isPresent()) {
            return;
        }
        PROFESSIONNELS.forEach(pro -> dossierService.accorderAcces(PATIENT, pro));
        log.info("Données de démo : dossier de {} créé, accès ACTIF pour {}", PATIENT, PROFESSIONNELS);
    }
}
