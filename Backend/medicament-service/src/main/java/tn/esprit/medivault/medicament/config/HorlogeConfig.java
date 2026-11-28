package tn.esprit.medivault.medicament.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class HorlogeConfig {

    @Bean
    public Clock horloge() {
        return Clock.systemDefaultZone();
    }
}
/// - Clock : l'horloge du service. On l'injecte au lieu d'appeler LocalDate.now() partout :
//   les tests peuvent ainsi choisir une date précise (changement de mois, par exemple).
// - @EnableScheduling : active les tâches planifiées (RG4, le 1er de chaque mois).