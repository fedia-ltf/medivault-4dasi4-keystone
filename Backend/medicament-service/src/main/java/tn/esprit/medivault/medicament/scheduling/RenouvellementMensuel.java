package tn.esprit.medivault.medicament.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.services.AttributionService;

// RG4 : "le 1er du mois : bilan + nouveaux quotas créés automatiquement".
// Cron "0 5 0 1 * *" = à 00h05, le 1er jour de chaque mois (modifiable dans application.properties).
@Component
@RequiredArgsConstructor
@Slf4j
public class RenouvellementMensuel {

    private final AttributionService attributions;

    @Scheduled(cron = "${medivault.quota.renouvellement-cron:0 5 0 1 * *}")
    public void renouveler() {
        log.info("RG4 : passage au nouveau mois");
        attributions.renouveler(attributions.moisCourant());
    }
}