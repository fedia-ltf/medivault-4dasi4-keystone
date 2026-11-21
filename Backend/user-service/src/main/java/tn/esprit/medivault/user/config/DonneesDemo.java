package tn.esprit.medivault.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.entities.Utilisateur;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

/**
 * Comptes fictifs du parcours de démonstration, avec des id fixes pour que les autres
 * microservices puissent les utiliser dans leurs tests (patient-ahmed, medecin-karim…).
 * Créés seulement si la collection est vide ; désactivable avec medivault.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "medivault.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DonneesDemo implements CommandLineRunner {

    private static final List<DayOfWeek> SEMAINE = List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) {
            return;
        }
        Instant maintenant = Instant.now();

        Utilisateur ahmed = Utilisateur.builder().id("patient-ahmed").nom("Ben Ali").prenom("Ahmed")
                .email("ahmed.benali@medivault.tn").telephone("+216 20 111 222").role(Role.PATIENT)
                .actif(true).dateCreation(maintenant).build();

        Utilisateur admin = Utilisateur.builder().id("admin").nom("Admin").prenom("MediVault")
                .email("admin@medivault.tn").role(Role.ADMIN).actif(true).dateCreation(maintenant).build();

        Professionnel karim = Professionnel.builder().id("medecin-karim").nom("Trabelsi").prenom("Karim")
                .email("karim.trabelsi@medivault.tn").telephone("+216 71 300 400").role(Role.MEDECIN)
                .actif(true).dateCreation(maintenant).type(TypeProfessionnel.MEDECIN)
                .nomEtablissement("Cabinet Dr Karim Trabelsi").adresse("12 rue de Marseille").ville("Tunis")
                .joursTravail(SEMAINE).heureDebut(LocalTime.of(8, 0)).heureFin(LocalTime.of(16, 0))
                .signatureImage(signatureDemo()).build();

        Professionnel bioLab = Professionnel.builder().id("labo-biolab").nom("BioLab").prenom("Laboratoire")
                .email("contact@biolab.tn").telephone("+216 71 500 600").role(Role.LABORANTIN)
                .actif(true).dateCreation(maintenant).type(TypeProfessionnel.LABORANTIN)
                .nomEtablissement("Laboratoire BioLab").numeroAgrement("LAB-2026-017")
                .adresse("5 avenue Habib Bourguiba").ville("Ariana")
                .joursTravail(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY))
                .heureDebut(LocalTime.of(7, 0)).heureFin(LocalTime.of(13, 0)).build();

        Professionnel pharmacie = Professionnel.builder().id("pharmacie-centrale").nom("Mansour").prenom("Sami")
                .email("pharmacie.centrale@medivault.tn").telephone("+216 71 700 800").role(Role.PHARMACIEN)
                .actif(true).dateCreation(maintenant).type(TypeProfessionnel.PHARMACIEN)
                .nomEtablissement("Pharmacie Centrale").numeroAgrement("PH-2026-042")
                .adresse("30 avenue de la Liberté").ville("Tunis").build();

        utilisateurRepository.saveAll(List.of(ahmed, admin, karim, bioLab, pharmacie));
        log.info("Données de démonstration créées : patient-ahmed, medecin-karim, labo-biolab, pharmacie-centrale, admin");
    }

    private String signatureDemo() {
        try (InputStream in = new ClassPathResource("demo/signature-karim.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            log.warn("Signature de démonstration introuvable : {}", e.getMessage());
            return null;
        }
    }
}
