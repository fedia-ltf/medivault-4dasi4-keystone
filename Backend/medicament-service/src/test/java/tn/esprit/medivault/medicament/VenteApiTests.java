package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.events.SanctionPharmacie;
import tn.esprit.medivault.medicament.events.VenteSensibleEnregistree;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// POST /api/ventes : les règles RG1 (ordonnance), RG2 (quota) et RG3 (avertissements, suspension).
// Ordonnances simulées : ORD-A7F3 et ORD-B219 (valides, 20 boîtes de chaque médicament), ORD-C884 (médicaments 1 et 3 seulement),
// ORD-D102 (en attente), ORD-E410 (délivrée). Pharmacie inactive : identifiant qui commence par "off-".
class VenteApiTests extends ApiTestBase {

    private Attribution recharger(Attribution a) {
        return attributions.findById(a.getId()).orElseThrow();
    }

    private void vendre(String pharmacie, String ordonnance, Long medicamentId, int quantite) throws Exception {
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json(pharmacie, ordonnance, medicamentId, quantite)));
    }

    @Test
    void venteEnregistree() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 50);

        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 5)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pharmacieId").value("1"))
                .andExpect(jsonPath("$.medicamentNom").value("Subutex 8 mg"))
                .andExpect(jsonPath("$.patientId").value("patient-1"))
                .andExpect(jsonPath("$.ordonnanceId").value(101))
                .andExpect(jsonPath("$.ordonnanceCode").value("ORD-A7F3"))
                .andExpect(jsonPath("$.quantite").value(5))
                .andExpect(jsonPath("$.date").exists());

        Attribution apres = recharger(quota);
        assertEquals(5, apres.getQuantiteVendue());
        assertEquals(45, apres.reste());
        assertEquals(0, apres.getAvertissements());
        assertEquals(1, ventes.count());

        // événement VenteSensibleEnregistree publié sur l'exchange medivault.events
        ArgumentCaptor<Object> evenement = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("medivault.events"), eq("vente.sensible.enregistree"), evenement.capture());
        VenteSensibleEnregistree e = (VenteSensibleEnregistree) evenement.getValue();
        assertEquals(101L, e.ordonnanceId());
        assertEquals(m.getId(), e.medicamentId());
        assertEquals(5, e.quantite());
        assertEquals("1", e.pharmacieId());
        assertNotNull(e.date());
    }

    @Test
    void lecodeDeLordonnanceNestPasSensibleALaCasse() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", " ord-a7f3 ", m.getId(), 1)))
                .andExpect(status().isCreated());
    }

    // RG1 : ordonnance non validée -> 409, 1 avertissement ; au 2e -> SUSPENDUE ; ensuite 403
    @Test
    void rg1PuisRg3_avertissementPuisSuspension() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 50);

        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-D102", m.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Vente non justifiée"))
                .andExpect(jsonPath("$.detail", containsString("n'est pas validée")))
                .andExpect(jsonPath("$.avertissements").value(1))
                .andExpect(jsonPath("$.suspendue").value(false));
        assertEquals(1, recharger(quota).getAvertissements());
        assertEquals(StatutAttribution.ACTIVE, recharger(quota).getStatut());

        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-E410", m.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.avertissements").value(2))
                .andExpect(jsonPath("$.suspendue").value(true));
        assertEquals(StatutAttribution.SUSPENDUE, recharger(quota).getStatut());

        // suspendue : même une vente parfaitement valable est refusée (403), sans nouvel avertissement
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Pharmacie suspendue"))
                .andExpect(jsonPath("$.suspendue").value(true));
        assertEquals(2, recharger(quota).getAvertissements());
        assertEquals(0, ventes.count());

        // événements SanctionPharmacie : un AVERTISSEMENT puis une SUSPENSION
        ArgumentCaptor<Object> evenements = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(2)).convertAndSend(eq("medivault.events"), eq("pharmacie.sanction"), evenements.capture());
        List<Object> envoyes = evenements.getAllValues();
        assertEquals(SanctionPharmacie.Niveau.AVERTISSEMENT, ((SanctionPharmacie) envoyes.get(0)).niveau());
        assertEquals(SanctionPharmacie.Niveau.SUSPENSION, ((SanctionPharmacie) envoyes.get(1)).niveau());
        assertEquals("1", ((SanctionPharmacie) envoyes.get(1)).pharmacieId());
        assertTrue(((SanctionPharmacie) envoyes.get(1)).motif().contains("Subutex 8 mg"));
        verify(rabbitTemplate, never()).convertAndSend(eq("medivault.events"), eq("vente.sensible.enregistree"), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void rg1_ordonnanceInconnue() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-0000", m.getId(), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("introuvable")));
        assertEquals(1, recharger(quota).getAvertissements());
    }

    @Test
    void rg1_lordonnanceNeContientPasCeMedicament() throws Exception {
        MedicamentSensible m1 = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        MedicamentSensible m2 = medicament("Méthadone 40 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m2, 50);
        // ORD-C884 ne contient que les médicaments 1 et 3
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-C884", m2.getId(), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("ne contient pas ce médicament")));
        assertEquals(1, recharger(quota).getAvertissements());
        // ... alors qu'elle justifie le médicament 1
        quota("1", m1, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-C884", m1.getId(), 1)))
                .andExpect(status().isCreated());
    }

    @Test
    void rg1_onNePeutPasDepasserLaQuantitePrescrite() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 100);
        // l'ordonnance prescrit 20 boîtes
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 15))).andExpect(status().isCreated());
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("supérieure à l'ordonnance")));
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 5))).andExpect(status().isCreated());
        assertEquals(20, recharger(quota).getQuantiteVendue());
        assertEquals(1, recharger(quota).getAvertissements());
    }

    // RG2 : quota du mois épuisé -> 409 + avertissement
    @Test
    void rg2_quotaInsuffisant() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 3);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Quota épuisé"))
                .andExpect(jsonPath("$.detail", containsString("il reste 3")))
                .andExpect(jsonPath("$.avertissements").value(1));
        assertEquals(0, recharger(quota).getQuantiteVendue());
        // vendre exactement le reste est permis
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 3))).andExpect(status().isCreated());
        assertEquals(0, recharger(quota).reste());
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-B219", m.getId(), 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.suspendue").value(true));
    }

    @Test
    void pharmacieInactiveRenvoie403SansAvertissement() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("off-1", m, 50);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("off-1", "ORD-A7F3", m.getId(), 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Pharmacie inactive"));
        assertEquals(0, recharger(quota).getAvertissements());
    }

    @Test
    void aucunQuotaCeMoisCiRenvoie403() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("7", "ORD-A7F3", m.getId(), 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Aucun quota"))
                .andExpect(jsonPath("$.avertissements").value(0));
    }

    @Test
    void unQuotaDunAutreMoisNeSertPas() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        attributions.save(Attribution.builder().pharmacieId("1").medicament(m).mois(java.time.YearMonth.now().minusMonths(1)).quantiteAttribuee(50).build());
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicamentInconnuRenvoie404() throws Exception {
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", 999L, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void donneesInvalidesRenvoient400() throws Exception {
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content("{\"pharmacieId\":\"\",\"quantite\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.pharmacieId").exists())
                .andExpect(jsonPath("$.erreurs.ordonnanceCode").exists())
                .andExpect(jsonPath("$.erreurs.medicamentId").exists())
                .andExpect(jsonPath("$.erreurs.quantite").exists());
    }

    @Test
    void historiqueDesVentesAvecFiltres() throws Exception {
        MedicamentSensible m1 = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        MedicamentSensible m2 = medicament("Skenan 30 mg", Categorie.STUPEFIANT);
        quota("1", m1, 50);
        quota("2", m1, 50);
        quota("1", m2, 50);
        vendre("1", "ORD-A7F3", m1.getId(), 2);
        vendre("2", "ORD-B219", m1.getId(), 3);
        vendre("1", "ORD-A7F3", m2.getId(), 4);

        mvc.perform(get("/api/ventes")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/ventes").param("pharmacieId", "1")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/ventes").param("medicamentId", m1.getId().toString())).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/ventes").param("pharmacieId", "2").param("medicamentId", m1.getId().toString()))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].ordonnanceCode").value("ORD-B219"));
        mvc.perform(get("/api/ventes").param("mois", java.time.YearMonth.now().toString())).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/ventes").param("mois", "2020-01")).andExpect(jsonPath("$", hasSize(0)));

        Long id = ventes.findAll().get(0).getId();
        mvc.perform(get("/api/ventes/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/ventes/999")).andExpect(status().isNotFound());
    }

    @Test
    void uneVenteNeSeModifiePasNeSeSupprimePas() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 50);
        vendre("1", "ORD-A7F3", m.getId(), 2);
        Long id = ventes.findAll().get(0).getId();
        mvc.perform(delete("/api/ventes/{id}", id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/ventes/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed());
    }
}
