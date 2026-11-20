package tn.esprit.medivault.analyse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.analyse.controller.AnalyseController;
import tn.esprit.medivault.analyse.dto.AnalyseResponse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.exception.AccesRefuseException;
import tn.esprit.medivault.analyse.exception.AnalyseNotFoundException;
import tn.esprit.medivault.analyse.exception.StatutInvalideException;
import tn.esprit.medivault.analyse.service.IAnalyseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyseController.class)
class AnalyseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAnalyseService service;

    private static final String SAISIE = """
            {"laboratoireId":"labo-biolab","resultats":[
              {"parametre":"Glycémie","valeur":1.30,"unite":"g/L","referenceMin":0.70,"referenceMax":1.10}]}
            """;

    private AnalyseResponse analyse(StatutAnalyse statut) {
        return new AnalyseResponse(1L, 45L, "patient-ahmed", "medecin-karim", "labo-biolab",
                "Bilan glycémique", statut, null, null, List.of());
    }

    @Test
    void get_parId_200_contratLuParRendezvous() throws Exception {
        when(service.trouverParId(1L)).thenReturn(analyse(StatutAnalyse.EN_ATTENTE));

        mockMvc.perform(get("/api/analyses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("patient-ahmed"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    void get_inconnue_404() throws Exception {
        when(service.trouverParId(99L)).thenThrow(new AnalyseNotFoundException(99L));

        mockMvc.perform(get("/api/analyses/99")).andExpect(status().isNotFound());
    }

    @Test
    void get_statutInconnu_400() throws Exception {
        mockMvc.perform(get("/api/analyses").param("statut", "FINIE")).andExpect(status().isBadRequest());
    }

    @Test
    void post_resultat_201_avecLocation() throws Exception {
        when(service.saisirResultat(eq(1L), any())).thenReturn(analyse(StatutAnalyse.TERMINEE));

        mockMvc.perform(post("/api/analyses/1/resultats").contentType(MediaType.APPLICATION_JSON).content(SAISIE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/analyses/1"))
                .andExpect(jsonPath("$.statut").value("TERMINEE"));
    }

    @Test
    void post_sansResultat_400() throws Exception {
        mockMvc.perform(post("/api/analyses/1/resultats").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratoireId\":\"labo-biolab\",\"resultats\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.resultats").exists());
    }

    @Test
    void post_rg7_409() throws Exception {
        when(service.saisirResultat(eq(1L), any())).thenThrow(new StatutInvalideException("RG7"));

        mockMvc.perform(post("/api/analyses/1/resultats").contentType(MediaType.APPLICATION_JSON).content(SAISIE))
                .andExpect(status().isConflict());
    }

    @Test
    void post_rg10_403() throws Exception {
        when(service.saisirResultat(eq(1L), any())).thenThrow(new AccesRefuseException("RG10"));

        mockMvc.perform(post("/api/analyses/1/resultats").contentType(MediaType.APPLICATION_JSON).content(SAISIE))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_dossierIndisponible_503() throws Exception {
        when(service.saisirResultat(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "dossier-service indisponible"));

        mockMvc.perform(post("/api/analyses/1/resultats").contentType(MediaType.APPLICATION_JSON).content(SAISIE))
                .andExpect(status().isServiceUnavailable());
    }
}
