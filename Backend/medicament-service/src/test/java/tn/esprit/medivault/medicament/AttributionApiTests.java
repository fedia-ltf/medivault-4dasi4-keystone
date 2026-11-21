package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.StatutAttribution;

import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// CRUD /api/attributions + bilan, lot, disponibilités et RG4 (nouveau mois)
class AttributionApiTests extends ApiTestBase {

    private String corps(String pharmacie, Long medicamentId, String mois, int quantite) {
        return """
                {"pharmacieId":"%s","medicamentId":%d,"mois":"%s","quantiteAttribuee":%d}""".formatted(pharmacie, medicamentId, mois, quantite);
    }

    @Test
    void creerPuisLire() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "2026-11", 50)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.pharmacieId").value("1"))
                .andExpect(jsonPath("$.medicamentNom").value("Subutex 8 mg"))
                .andExpect(jsonPath("$.mois").value("2026-11"))
                .andExpect(jsonPath("$.quantiteAttribuee").value(50))
                .andExpect(jsonPath("$.quantiteVendue").value(0))
                .andExpect(jsonPath("$.reste").value(50))
                .andExpect(jsonPath("$.avertissements").value(0))
                .andExpect(jsonPath("$.statut").value("ACTIVE"));

        Long id = attributions.findAll().get(0).getId();
        mvc.perform(get("/api/attributions/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.quantiteAttribuee").value(50));
    }

    @Test
    void unSeulQuotaParPharmacieMedicamentEtMois() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "2026-11", 50))).andExpect(status().isCreated());
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "2026-11", 80))).andExpect(status().isConflict());
        // un autre mois ou une autre pharmacie est permis
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "2026-12", 80))).andExpect(status().isCreated());
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("2", m.getId(), "2026-11", 80))).andExpect(status().isCreated());
    }

    @Test
    void erreursDeCreation() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", 999L, "2026-11", 50))).andExpect(status().isNotFound());
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "2026-11", 0)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreurs.quantiteAttribuee").exists());
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps(" ", m.getId(), "2026-11", 5)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreurs.pharmacieId").exists());
        mvc.perform(post("/api/attributions").contentType(MediaType.APPLICATION_JSON).content(corps("1", m.getId(), "novembre", 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifierLaQuantite() throws Exception {
        Attribution a = quota("1", medicament("Subutex 8 mg", Categorie.STUPEFIANT), 50);
        mvc.perform(put("/api/attributions/{id}", a.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"quantiteAttribuee\":80}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantiteAttribuee").value(80)).andExpect(jsonPath("$.reste").value(80));
    }

    @Test
    void lequotaNePeutPasPasserSousLesBoitesDejaVendues() throws Exception {
        Attribution a = quota("1", medicament("Subutex 8 mg", Categorie.STUPEFIANT), 50);
        a.setQuantiteVendue(30);
        attributions.save(a);
        mvc.perform(put("/api/attributions/{id}", a.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"quantiteAttribuee\":20}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/attributions/{id}", a.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"quantiteAttribuee\":30}"))
                .andExpect(status().isOk());
    }

    @Test
    void remettreActiveLeveLaSanction() throws Exception {
        Attribution a = quota("1", medicament("Subutex 8 mg", Categorie.STUPEFIANT), 50);
        a.ajouterAvertissement();
        a.ajouterAvertissement();
        attributions.save(a);
        assertEquals(StatutAttribution.SUSPENDUE, attributions.findById(a.getId()).orElseThrow().getStatut());

        mvc.perform(put("/api/attributions/{id}", a.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantiteAttribuee\":50,\"statut\":\"ACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("ACTIVE")).andExpect(jsonPath("$.avertissements").value(0));
    }

    @Test
    void inconnuRenvoie404() throws Exception {
        mvc.perform(get("/api/attributions/999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/attributions/999").contentType(MediaType.APPLICATION_JSON).content("{\"quantiteAttribuee\":5}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/attributions/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/attributions/999/bilan")).andExpect(status().isNotFound());
    }

    @Test
    void supprimer() throws Exception {
        Attribution a = quota("1", medicament("Subutex 8 mg", Categorie.STUPEFIANT), 50);
        mvc.perform(delete("/api/attributions/{id}", a.getId())).andExpect(status().isNoContent());
        assertEquals(0, attributions.count());
    }

    @Test
    void supprimerUnQuotaAvecDesVentesRenvoie409() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution a = quota("1", m, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 2))).andExpect(status().isCreated());
        mvc.perform(delete("/api/attributions/{id}", a.getId())).andExpect(status().isConflict());
    }

    @Test
    void quotaEnLot() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("2", m, 50);
        mvc.perform(post("/api/attributions/lot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicamentId\":%d,\"mois\":\"%s\",\"quantiteAttribuee\":40,\"pharmacieIds\":[\"1\",\"2\",\"3\",\"3\"]}".formatted(m.getId(), YearMonth.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.crees").value(2))
                .andExpect(jsonPath("$.ignores").value(1))
                .andExpect(jsonPath("$.pharmaciesIgnorees[0]").value("2"));
        assertEquals(3, attributions.count());
        mvc.perform(post("/api/attributions/lot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicamentId\":%d,\"mois\":\"%s\",\"quantiteAttribuee\":40,\"pharmacieIds\":[]}".formatted(m.getId(), YearMonth.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bilan() throws Exception {
        Attribution a = quota("1", medicament("Subutex 8 mg", Categorie.STUPEFIANT), 50);
        a.setQuantiteVendue(20);
        a.ajouterAvertissement();
        attributions.save(a);
        mvc.perform(get("/api/attributions/{id}/bilan", a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recu").value(50))
                .andExpect(jsonPath("$.vendu").value(20))
                .andExpect(jsonPath("$.reste").value(30))
                .andExpect(jsonPath("$.avertissements").value(1))
                .andExpect(jsonPath("$.statut").value("ACTIVE"));
    }

    @Test
    void filtres() throws Exception {
        MedicamentSensible m1 = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        MedicamentSensible m2 = medicament("Ritaline", Categorie.PSYCHOTROPE);
        quota("1", m1, 50);
        quota("2", m1, 50);
        Attribution suspendue = quota("1", m2, 50);
        suspendue.setStatut(StatutAttribution.SUSPENDUE);
        attributions.save(suspendue);
        attributions.save(Attribution.builder().pharmacieId("1").medicament(m2).mois(YearMonth.of(2020, 1)).quantiteAttribuee(5).build());

        mvc.perform(get("/api/attributions")).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/attributions").param("pharmacieId", "1")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/attributions").param("medicamentId", m1.getId().toString())).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/attributions").param("mois", "2020-01")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/attributions").param("statut", "SUSPENDUE")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/attributions").param("pharmacieId", "1").param("mois", YearMonth.now().toString())).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void disponibilites() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 100);                                  // DISPONIBLE
        Attribution presqueVide = quota("2", m, 100);        // BIENTOT_EPUISE : reste 10 %
        presqueVide.setQuantiteVendue(90);
        Attribution epuisee = quota("3", m, 100);            // INDISPONIBLE : plus rien
        epuisee.setQuantiteVendue(100);
        Attribution suspendue = quota("4", m, 100);          // INDISPONIBLE : suspendue
        suspendue.setStatut(StatutAttribution.SUSPENDUE);
        attributions.saveAll(java.util.List.of(presqueVide, epuisee, suspendue));

        mvc.perform(get("/api/disponibilites").param("medicamentId", m.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].pharmacieId").value("1"))
                .andExpect(jsonPath("$[0].etat").value("DISPONIBLE"))
                .andExpect(jsonPath("$[1].etat").value("BIENTOT_EPUISE"))
                .andExpect(jsonPath("$[1].reste").value(10))
                .andExpect(jsonPath("$[2].etat").value("INDISPONIBLE"))
                .andExpect(jsonPath("$[3].etat").value("INDISPONIBLE"));
        mvc.perform(get("/api/disponibilites").param("medicamentId", "999")).andExpect(status().isNotFound());
    }

    // RG4 : le nouveau mois reprend les quotas du mois précédent, ACTIVE, sans vente ni avertissement
    @Test
    void renouvellementDuMois() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        YearMonth ancien = YearMonth.of(2026, 10);
        Attribution a = attributions.save(Attribution.builder().pharmacieId("1").medicament(m).mois(ancien).quantiteAttribuee(60).build());
        a.setQuantiteVendue(55);
        a.ajouterAvertissement();
        a.ajouterAvertissement();
        attributions.save(a);
        attributions.save(Attribution.builder().pharmacieId("2").medicament(m).mois(ancien).quantiteAttribuee(30).build());
        attributions.save(Attribution.builder().pharmacieId("2").medicament(m).mois(YearMonth.of(2026, 11)).quantiteAttribuee(30).build()); // existe déjà

        mvc.perform(post("/api/attributions/renouvellement").param("mois", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moisBilan").value("2026-10"))
                .andExpect(jsonPath("$.moisNouveau").value("2026-11"))
                .andExpect(jsonPath("$.crees").value(1))
                .andExpect(jsonPath("$.dejaExistants").value(1))
                .andExpect(jsonPath("$.bilans", hasSize(2)))
                .andExpect(jsonPath("$.bilans[?(@.pharmacieId=='1')].vendu").value(55));

        mvc.perform(get("/api/attributions").param("pharmacieId", "1").param("mois", "2026-11"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantiteAttribuee").value(60))
                .andExpect(jsonPath("$[0].quantiteVendue").value(0))
                .andExpect(jsonPath("$[0].avertissements").value(0))
                .andExpect(jsonPath("$[0].statut").value("ACTIVE"));

        // relancer ne crée rien de plus
        mvc.perform(post("/api/attributions/renouvellement").param("mois", "2026-11"))
                .andExpect(jsonPath("$.crees").value(0)).andExpect(jsonPath("$.dejaExistants").value(2));
    }
}
