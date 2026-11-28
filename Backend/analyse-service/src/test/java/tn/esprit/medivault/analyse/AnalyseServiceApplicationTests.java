package tn.esprit.medivault.analyse;

import org.junit.jupiter.api.Test;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test léger sans contexte Spring (pas besoin d'Eureka, de RabbitMQ ni du Config Server). */
class AnalyseServiceApplicationTests {

    @Test
    void nouvelleAnalyseEstEnAttenteSansResultat() {
        Analyse analyse = Analyse.builder()
                .patientId("patient-ahmed")
                .medecinId("medecin-karim")
                .typeAnalyse("Bilan sanguin")
                .statut(StatutAnalyse.EN_ATTENTE)
                .build();

        assertEquals(StatutAnalyse.EN_ATTENTE, analyse.getStatut());
        assertTrue(analyse.getResultats().isEmpty());
    }
}
