package tn.esprit.medivault.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.medivault.user.controllers.UtilisateurController;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.exceptions.EmailDejaUtiliseException;
import tn.esprit.medivault.user.exceptions.UtilisateurNotFoundException;
import tn.esprit.medivault.user.services.IUtilisateurService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UtilisateurController.class)
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUtilisateurService service;

    private static final String PATIENT = """
            {"nom":"Ben Ali","prenom":"Ahmed","email":"ahmed@test.tn","motDePasse":"motdepasse","role":"PATIENT"}
            """;

    @Test
    void post_201_avecLocation() throws Exception {
        when(service.creer(any())).thenReturn(new UtilisateurResponse("u1", "Ben Ali", "Ahmed", "ahmed@test.tn",
                null, Role.PATIENT, true, null, null, null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(PATIENT))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/utilisateurs/u1"))
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    void post_emailInvalide_400() throws Exception {
        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"A\",\"prenom\":\"B\",\"email\":\"pas-un-email\",\"role\":\"PATIENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.email").exists());
    }

    @Test
    void post_roleInconnu_400() throws Exception {
        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"A\",\"prenom\":\"B\",\"email\":\"a@b.tn\",\"role\":\"INFIRMIER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_emailDejaUtilise_409() throws Exception {
        when(service.creer(any())).thenThrow(new EmailDejaUtiliseException("ahmed@test.tn"));

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(PATIENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email déjà utilisé"));
    }

    @Test
    void get_inconnu_404() throws Exception {
        when(service.trouverParId("x")).thenThrow(new UtilisateurNotFoundException("Aucun utilisateur avec l'id x"));

        mockMvc.perform(get("/api/utilisateurs/x")).andExpect(status().isNotFound());
    }
}
