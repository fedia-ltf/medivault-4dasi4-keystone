package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;
import tn.esprit.medivault.medicament.entities.Medicament;
import tn.esprit.medivault.medicament.messaging.MedicamentEventPublisher;
import tn.esprit.medivault.medicament.messaging.OrdonnanceDelivreeListener;
import tn.esprit.medivault.medicament.repositories.MedicamentRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MedicamentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicamentRepository medicamentRepository;

    // RabbitMQ n'est pas disponible pendant les tests
    @MockitoBean
    private MedicamentEventPublisher eventPublisher;

    private Long idDe(String nomCommercial) {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getNom_commercial().equals(nomCommercial))
                .map(Medicament::getId)
                .findFirst().orElseThrow();
    }

    @Autowired
    private OrdonnanceDelivreeListener ordonnanceDelivreeListener;

    @Test
    void ordonnanceDelivreeIncrementeNbDelivrances() {
        Long id = idDe("Doliprane");
        OrdonnanceDelivreeEvent event = new OrdonnanceDelivreeEvent(1L, 5L,
                List.of(new OrdonnanceDelivreeEvent.LigneDelivree(id, 2)), "TOTALE");

        ordonnanceDelivreeListener.onOrdonnanceDelivree(event);
        ordonnanceDelivreeListener.onOrdonnanceDelivree(event);

        assertEquals(2, medicamentRepository.findById(id).orElseThrow().getNb_delivrances());
    }

    @Test
    void catalogueChargeDepuisDataSql() throws Exception {
        mockMvc.perform(get("/api/medicaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(31))
                .andExpect(jsonPath("$[0].nom_commercial").exists());
    }

    @Test
    void ajoutValideRenvoie201() throws Exception {
        String body = """
                {"nom_commercial":"Spasfon","dci":"Phloroglucinol","classe_therapeutique":"Antispasmodique",
                 "forme":"COMPRIME","dosage":"80 mg"}""";
        mockMvc.perform(post("/api/medicaments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void ajoutInvalideRenvoie400() throws Exception {
        mockMvc.perform(post("/api/medicaments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dci\":\"\",\"forme\":\"COMPRIME\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erreurs.nom_commercial").exists());
    }

    @Test
    void allergieMemeClasseRendIncompatible() throws Exception {
        String body = "{\"medicament_ids\":[" + idDe("Augmentin") + "," + idDe("Doliprane")
                + "],\"allergies\":[\"penicilline\"]}";
        mockMvc.perform(post("/api/medicaments/compatibilite").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.alertes[0].type").value("ALLERGIE"));
    }

    @Test
    void interactionContreIndiqueeRendIncompatible() throws Exception {
        String body = "{\"medicament_ids\":[" + idDe("Viagra") + "," + idDe("Risordan") + "],\"allergies\":[]}";
        mockMvc.perform(post("/api/medicaments/compatibilite").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.alertes[0].gravite").value("CONTRE_INDIQUEE"));
    }

    @Test
    void interactionGraveDonneAlerteMaisResteCompatible() throws Exception {
        String body = "{\"medicament_ids\":[" + idDe("Coumadine") + "," + idDe("Advil") + "],\"allergies\":[]}";
        mockMvc.perform(post("/api/medicaments/compatibilite").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.compatible").value(true))
                .andExpect(jsonPath("$.alertes[0].gravite").value("GRAVE"));
    }

    @Test
    void retraitPuisMedicamentNonPrescriptible() throws Exception {
        Long id = idDe("Zocor");
        mockMvc.perform(delete("/api/medicaments/" + id).param("motif", "Rappel de lot"))
                .andExpect(status().isNoContent());
        verify(eventPublisher).publierMedicamentRetire(any());

        mockMvc.perform(post("/api/medicaments/compatibilite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicament_ids\":[" + id + "]}"))
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.alertes[0].type").value("MEDICAMENT_RETIRE"));

        mockMvc.perform(delete("/api/medicaments/" + id))
                .andExpect(status().isNotFound());
    }
}
