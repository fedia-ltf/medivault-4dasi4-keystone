package tn.esprit.medivault.analyse.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.entity.ResultatAnalyse;
import tn.esprit.medivault.analyse.enums.Interpretation;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.repository.AnalyseRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Analyses du parcours d'Ahmed, avec les id de démonstration de user-service
 * (patient-ahmed, medecin-karim, labo-biolab). Désactivable : medivault.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "medivault.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DonneesDemo implements CommandLineRunner {

    private final AnalyseRepository analyseRepository;

    @Override
    public void run(String... args) {
        if (analyseRepository.count() > 0) {
            return;
        }
        LocalDateTime maintenant = LocalDateTime.now();

        Analyse glycemie = Analyse.builder().consultationId(45L).patientId("patient-ahmed").medecinId("medecin-karim")
                .laboratoireId("labo-biolab").typeAnalyse("Bilan glycémique").statut(StatutAnalyse.EN_COURS)
                .dateDemande(maintenant.minusDays(1)).build();

        Analyse nfs = Analyse.builder().consultationId(45L).patientId("patient-ahmed").medecinId("medecin-karim")
                .typeAnalyse("NFS").statut(StatutAnalyse.EN_ATTENTE).dateDemande(maintenant.minusDays(1)).build();

        Analyse lipidique = Analyse.builder().consultationId(12L).patientId("patient-ahmed").medecinId("medecin-karim")
                .laboratoireId("labo-biolab").typeAnalyse("Bilan lipidique").statut(StatutAnalyse.TERMINEE)
                .dateDemande(maintenant.minusMonths(2)).dateResultat(maintenant.minusMonths(2).plusDays(2)).build();
        lipidique.ajouterResultat(ResultatAnalyse.builder().parametre("Cholestérol total").valeur(1.80).unite("g/L")
                .valeurReference("1,50–2,00").interpretation(Interpretation.NORMAL).build());

        analyseRepository.saveAll(List.of(glycemie, nfs, lipidique));
        log.info("Données de démonstration : analyse {} EN_COURS (BioLab), {} EN_ATTENTE, {} TERMINEE",
                glycemie.getId(), nfs.getId(), lipidique.getId());
    }
}
