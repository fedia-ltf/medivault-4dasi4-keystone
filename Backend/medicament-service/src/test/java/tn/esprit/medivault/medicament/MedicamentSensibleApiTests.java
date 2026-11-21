package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// CRUD /api/medicaments-sensibles
class MedicamentSensibleApiTests extends ApiTestBase {

    private static final String CORPS = """
            {"nomCommercial":"Subutex 8 mg","dci":"Buprénorphine","categorie":"STUPEFIANT",
             "description":"Traitement de substitution","precautions":"Pas d'alcool"}""";

    @Test
    void creerPuisLire() throws Exception {
        mvc.perform(post("/api/medicaments-sensibles").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomCommercial").value("Subutex 8 mg"))
                .andExpect(jsonPath("$.categorie").value("STUPEFIANT"));

        Long id = medicaments.findAll().get(0).getId();
        mvc.perform(get("/api/medicaments-sensibles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dci").value("Buprénorphine"))
                .andExpect(jsonPath("$.precautions").value("Pas d'alcool"));
        mvc.perform(get("/api/medicaments-sensibles")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void doublonRenvoie409MemeSiLaCasseChange() throws Exception {
        medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(post("/api/medicaments-sensibles").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.replace("Subutex 8 mg", "SUBUTEX 8 MG")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ce médicament existe déjà."));
    }

    @Test
    void donneesInvalidesRenvoient400() throws Exception {
        mvc.perform(post("/api/medicaments-sensibles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomCommercial\":\"  \",\"dci\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.nomCommercial").exists())
                .andExpect(jsonPath("$.erreurs.dci").exists())
                .andExpect(jsonPath("$.erreurs.categorie").exists());
    }

    @Test
    void categorieInconnueRenvoie400() throws Exception {
        mvc.perform(post("/api/medicaments-sensibles").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.replace("STUPEFIANT", "VITAMINE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifier() throws Exception {
        MedicamentSensible m = medicament("Ritaline", Categorie.PSYCHOTROPE);
        mvc.perform(put("/api/medicaments-sensibles/{id}", m.getId()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomCommercial").value("Subutex 8 mg"))
                .andExpect(jsonPath("$.categorie").value("STUPEFIANT"));
    }

    @Test
    void modifierEnPrenantLeNomDunAutreRenvoie409() throws Exception {
        medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        MedicamentSensible autre = medicament("Ritaline", Categorie.PSYCHOTROPE);
        mvc.perform(put("/api/medicaments-sensibles/{id}", autre.getId()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isConflict());
    }

    @Test
    void modifierEnGardantSonNomEstPermis() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(put("/api/medicaments-sensibles/{id}", m.getId()).contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk());
    }

    @Test
    void inconnuRenvoie404() throws Exception {
        mvc.perform(get("/api/medicaments-sensibles/999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/medicaments-sensibles/999").contentType(MediaType.APPLICATION_JSON).content(CORPS)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/medicaments-sensibles/999")).andExpect(status().isNotFound());
    }

    @Test
    void supprimer() throws Exception {
        MedicamentSensible m = medicament("Xanax", Categorie.PSYCHOTROPE);
        mvc.perform(delete("/api/medicaments-sensibles/{id}", m.getId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/medicaments-sensibles/{id}", m.getId())).andExpect(status().isNotFound());
    }

    @Test
    void supprimerUnMedicamentAttribueRenvoie409() throws Exception {
        MedicamentSensible m = medicament("Xanax", Categorie.PSYCHOTROPE);
        quota("1", m, 50);
        mvc.perform(delete("/api/medicaments-sensibles/{id}", m.getId())).andExpect(status().isConflict());
    }

    @Test
    void filtrerParCategorieEtParTexte() throws Exception {
        medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        medicament("Ritaline", Categorie.PSYCHOTROPE);
        medicament("Misoprostol", Categorie.ABORTIF);
        mvc.perform(get("/api/medicaments-sensibles").param("categorie", "PSYCHOTROPE"))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].nomCommercial").value("Ritaline"));
        mvc.perform(get("/api/medicaments-sensibles").param("q", "subu"))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].nomCommercial").value("Subutex 8 mg"));
        mvc.perform(get("/api/medicaments-sensibles").param("q", "DCI misoprostol"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/medicaments-sensibles").param("categorie", "AUTRE")).andExpect(status().isBadRequest());
    }
}
