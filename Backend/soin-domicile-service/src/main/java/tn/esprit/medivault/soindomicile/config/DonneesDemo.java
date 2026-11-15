package tn.esprit.medivault.soindomicile.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.soindomicile.entities.Infirmier;
import tn.esprit.medivault.soindomicile.repositories.InfirmierRepository;

import java.time.LocalTime;
import java.util.List;

/** H2 est vidée à chaque démarrage : on remet trois infirmiers de démonstration. */
@Component
@RequiredArgsConstructor
public class DonneesDemo implements CommandLineRunner {

    private final InfirmierRepository infirmiers;

    @Override
    public void run(String... args) {
        if (infirmiers.count() > 0) return;
        infirmiers.saveAll(List.of(
                infirmier("Sonia", "Ben Ali", "+216 22 145 870", 8, 16),
                infirmier("Karim", "Jaziri", "+216 98 330 214", 12, 20),
                infirmier("Leila", "Mansour", "+216 55 702 961", 7, 13)));
    }

    private Infirmier infirmier(String prenom, String nom, String tel, int debut, int fin) {
        return Infirmier.builder().prenom(prenom).nom(nom).telephone(tel).pharmacieId("ph-ennasr")
                .heureDebut(LocalTime.of(debut, 0)).heureFin(LocalTime.of(fin, 0)).build();
    }
}
