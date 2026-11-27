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

        List<Utilisateur> tous = new java.util.ArrayList<>(List.of(ahmed, admin, karim, bioLab, pharmacie));
        tous.add(pharmacie("pharmacie-marsa", "Trabelsi", "Leila", "marsa@medivault.tn", "+216 71 740 100",
                "Pharmacie de la Marsa", "PH-2026-043", "12 avenue Habib Bourguiba", "La Marsa", maintenant));
        tous.add(pharmacie("pharmacie-manar", "Gharbi", "Karim", "manar@medivault.tn", "+216 71 750 200",
                "Pharmacie El Manar", "PH-2026-044", "5 rue des Orangers", "Tunis", maintenant));
        tous.add(pharmacie("pharmacie-ibnsina", "Jaziri", "Nadia", "ibnsina@medivault.tn", "+216 74 260 300",
                "Pharmacie Ibn Sina", "PH-2026-045", "21 avenue Hedi Chaker", "Sfax", maintenant));
        tous.add(pharmacie("pharmacie-jasmins", "Mejri", "Walid", "jasmins@medivault.tn", "+216 73 270 400",
                "Pharmacie Les Jasmins", "PH-2026-046", "8 boulevard du 14 Janvier", "Sousse", maintenant));
        tous.add(pharmacie("pharmacie-carthage", "Hamdi", "Sonia", "carthage@medivault.tn", "+216 71 730 500",
                "Pharmacie Carthage", "PH-2026-047", "3 rue d'Hannibal", "Carthage", maintenant));
        tous.add(pharmacie("pharmacie-menzah", "Dridi", "Amine", "menzah@medivault.tn", "+216 71 760 600",
                "Pharmacie El Menzah", "PH-2026-048", "17 avenue de l'Environnement", "Tunis", maintenant));
        tous.add(pharmacie("pharmacie-bizerte", "Chaabane", "Rim", "bizerte@medivault.tn", "+216 72 440 700",
                "Pharmacie Bizerte Centre", "PH-2026-049", "10 rue de Palestine", "Bizerte", maintenant));
        utilisateurRepository.saveAll(tous);
        log.info("Données de démonstration créées : {} comptes (patient, admin, médecin, laboratoire, 8 pharmacies)", tous.size());
    }

    private Professionnel pharmacie(String id, String nom, String prenom, String email, String telephone,
                                    String etablissement, String agrement, String adresse, String ville, Instant creation) {
        return Professionnel.builder().id(id).nom(nom).prenom(prenom).email(email).telephone(telephone)
                .role(Role.PHARMACIEN).actif(true).dateCreation(creation).type(TypeProfessionnel.PHARMACIEN)
                .nomEtablissement(etablissement).numeroAgrement(agrement).adresse(adresse).ville(ville).build();
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
