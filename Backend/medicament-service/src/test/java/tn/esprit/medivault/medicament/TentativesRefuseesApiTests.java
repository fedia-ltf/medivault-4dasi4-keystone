package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// L'historique des ventes refusées : chaque refus de POST /api/ventes laisse une trace consultable.
class TentativesRefuseesApiTests extends ApiTestBase {

    private void vendre(String pharmacie, String ordonnance, Long medicamentId, int quantite) throws Exception {
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json(pharmacie, ordonnance, medicamentId, quantite)));
    }

    @Test
    void unQuotaEpuiseLaisseUneTrace() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 3);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 5)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/tentatives-refusees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pharmacieId").value("1"))
                .andExpect(jsonPath("$[0].medicamentId").value(m.getId()))
                .andExpect(jsonPath("$[0].medicamentNom").value("Subutex 8 mg"))
                .andExpect(jsonPath("$[0].ordonnanceCode").value("ORD-A7F3"))
                .andExpect(jsonPath("$[0].quantite").value(5))
                .andExpect(jsonPath("$[0].motif").value("QUOTA_EPUISE"))
                .andExpect(jsonPath("$[0].detail", containsString("Quota du mois insuffisant")))
                .andExpect(jsonPath("$[0].date").exists());
    }

    @Test
    void chaqueMotifDeRefusEstEnregistre() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 50);
        Attribution suspendue = quota("2", m, 50);
        suspendue.setStatut(StatutAttribution.SUSPENDUE);
        attributions.save(suspendue);

        vendre("off-1", "ORD-A7F3", m.getId(), 1);   // pharmacie inactive
        vendre("9", "ORD-A7F3", m.getId(), 1);       // aucun quota pour cette pharmacie
        vendre("2", "ORD-A7F3", m.getId(), 1);       // quota suspendu
        vendre("1", "ORD-D102", m.getId(), 1);       // ordonnance en attente : non justifiée
        vendre("1", "ORD-INCONNUE", m.getId(), 1);   // ordonnance introuvable : non justifiée

        var motifs = tentatives.findAll().stream().map(TentativeRefusee::getMotif).toList();
        assertEquals(5, motifs.size());
        assertTrue(motifs.containsAll(java.util.List.of(MotifRefus.PHARMACIE_INACTIVE, MotifRefus.AUCUN_QUOTA,
                MotifRefus.PHARMACIE_SUSPENDUE, MotifRefus.ORDONNANCE_NON_JUSTIFIEE)));
        assertEquals(2, motifs.stream().filter(x -> x == MotifRefus.ORDONNANCE_NON_JUSTIFIEE).count());
    }

    @Test
    void uneVenteAccepteeNeLaisseAucuneTrace() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 5)))
                .andExpect(status().isCreated());
        assertEquals(0, tentatives.count());
    }

    @Test
    void unMedicamentInconnuNeLaisseAucuneTrace() throws Exception {
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", 999L, 1)))
                .andExpect(status().isNotFound());
        assertEquals(0, tentatives.count());
    }

    @Test
    void lHistoriqueSeFiltreParPharmacieEtParMotif() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 1);
        quota("2", m, 50);
        vendre("1", "ORD-A7F3", m.getId(), 5);       // quota épuisé (pharmacie 1)
        vendre("2", "ORD-D102", m.getId(), 1);       // ordonnance non justifiée (pharmacie 2)

        mvc.perform(get("/api/tentatives-refusees").param("pharmacieId", "2"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].motif").value("ORDONNANCE_NON_JUSTIFIEE"));
        mvc.perform(get("/api/tentatives-refusees").param("motif", "QUOTA_EPUISE"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pharmacieId").value("1"));
        mvc.perform(get("/api/tentatives-refusees").param("medicamentId", "999"))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/tentatives-refusees").param("mois", java.time.YearMonth.now().toString()))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/tentatives-refusees").param("mois", "2020-01"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void lesPlusRecentsSontEnPremier() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 1);
        vendre("1", "ORD-A7F3", m.getId(), 5);
        vendre("1", "ORD-A7F3", m.getId(), 7);
        mvc.perform(get("/api/tentatives-refusees"))
                .andExpect(jsonPath("$[0].quantite").value(7))
                .andExpect(jsonPath("$[1].quantite").value(5));
    }
}
