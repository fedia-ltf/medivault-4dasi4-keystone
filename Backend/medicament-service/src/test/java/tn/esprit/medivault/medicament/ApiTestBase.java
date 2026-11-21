package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;
import tn.esprit.medivault.medicament.repositories.VenteSensibleRepository;

import java.time.YearMonth;

// Base commune des tests d'API : une vraie base H2 (en mémoire), de vraies requêtes HTTP simulées (MockMvc).
// Les autres microservices sont simulés (medivault.integration.mode=simule) et RabbitMQ est remplacé par un faux.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medicament_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "medivault.demo-data=false",
        "medivault.integration.mode=simule",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureMockMvc
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected MedicamentSensibleRepository medicaments;
    @Autowired
    protected AttributionRepository attributions;
    @Autowired
    protected VenteSensibleRepository ventes;
    @Autowired
    private JdbcTemplate jdbc;

    // RabbitMQ n'est pas disponible pendant les tests : on vérifie seulement ce qui serait publié
    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    // Avant chaque test : base vide, et les identifiants repartent de 1
    // (les ordonnances simulées parlent des médicaments 1, 2, 3...)
    @BeforeEach
    void viderLaBase() {
        ventes.deleteAll();
        attributions.deleteAll();
        medicaments.deleteAll();
        jdbc.execute("ALTER TABLE medicament_sensible ALTER COLUMN id RESTART WITH 1");
    }

    protected MedicamentSensible medicament(String nom, Categorie categorie) {
        return medicaments.save(MedicamentSensible.builder().nomCommercial(nom).dci("DCI " + nom).categorie(categorie).build());
    }

    protected Attribution quota(String pharmacieId, MedicamentSensible medicament, int quantite) {
        return attributions.save(Attribution.builder().pharmacieId(pharmacieId).medicament(medicament)
                .mois(YearMonth.now()).quantiteAttribuee(quantite).build());
    }

    protected String json(String pharmacieId, String ordonnance, Long medicamentId, int quantite) {
        return """
                {"pharmacieId":"%s","ordonnanceCode":"%s","medicamentId":%d,"quantite":%d}"""
                .formatted(pharmacieId, ordonnance, medicamentId, quantite);
    }
}
