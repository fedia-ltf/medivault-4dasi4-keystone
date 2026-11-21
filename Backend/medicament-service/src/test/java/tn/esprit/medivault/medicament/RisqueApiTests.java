package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.dto.RisqueDtos.Niveau;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;
import tn.esprit.medivault.medicament.entities.VenteSensible;
import tn.esprit.medivault.medicament.services.RisqueService;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Le score de risque : le calcul (somme de points expliqués), puis l'API, puis le tout de bout en bout (vrais refus).
class RisqueApiTests extends ApiTestBase {

    private void refus(String pharmacieId, MedicamentSensible m, int nombre) {
        for (int i = 0; i < nombre; i++) {
            tentatives.save(TentativeRefusee.builder().pharmacieId(pharmacieId).medicamentId(m.getId()).medicamentNom(m.getNomCommercial())
                    .ordonnanceCode("ORD-X").quantite(1).date(LocalDateTime.now()).motif(MotifRefus.QUOTA_EPUISE).build());
        }
    }

    // ---------- le calcul seul ----------

    @Test
    void uneCalmePharmacieABienUnScoreNul() {
        var r = RisqueService.evaluer("1", 0, 0, 0, 100, 20, 0, true, 10);
        assertEquals(0, r.score());
        assertEquals(Niveau.FAIBLE, r.niveau());
        assertTrue(r.raisons().isEmpty());
    }

    @Test
    void lesPointsSAdditionnentEtChaqueRaisonEstExpliquee() {
        // 3 refus (36) + 2 avertissements (30) + 1 suspension (25) = 91
        var r = RisqueService.evaluer("1", 3, 2, 1, 100, 30, 0, true, 25);
        assertEquals(91, r.score());
        assertEquals(Niveau.ELEVE, r.niveau());
        assertEquals(3, r.raisons().size());
        assertEquals("3 ventes refusées ce mois-ci", r.raisons().get(0));
        assertEquals("2 avertissements", r.raisons().get(1));
        assertEquals("Suspendue pour 1 médicament", r.raisons().get(2));
    }

    @Test
    void lesPlafondsEtLeScoreMaximumSontRespectes() {
        // refus plafonnés à 40, avertissements à 30 ; 40 + 30 + 25 + 20 + 10 = 125 -> plafonné à 100
        var r = RisqueService.evaluer("1", 10, 5, 2, 100, 95, 60, true, 12);
        assertEquals(100, r.score());
    }

    @Test
    void lesVentesDeFinDeMoisPesent20Points() {
        var r = RisqueService.evaluer("1", 0, 0, 0, 100, 40, 20, false, 30);   // 50 % des boîtes dans les 5 derniers jours
        assertEquals(20, r.score());
        assertEquals(Niveau.FAIBLE, r.niveau());
        assertEquals("50 % des boîtes vendues dans les 5 derniers jours du mois", r.raisons().get(0));
        // trop peu de boîtes vendues : aucun soupçon
        assertEquals(0, RisqueService.evaluer("1", 0, 0, 0, 100, 8, 8, false, 30).score());
        // moins de 40 % en fin de mois : normal
        assertEquals(0, RisqueService.evaluer("1", 0, 0, 0, 100, 40, 10, false, 30).score());
    }

    @Test
    void unQuotaConsommeTropViteAjoute10Points() {
        var r = RisqueService.evaluer("1", 0, 0, 0, 100, 93, 0, true, 15);
        assertEquals(10, r.score());
        assertEquals("Quota consommé à 93 % avant le 20 du mois", r.raisons().get(0));
        assertEquals(0, RisqueService.evaluer("1", 0, 0, 0, 100, 93, 0, true, 25).score());   // après le 20 : normal
        assertEquals(0, RisqueService.evaluer("1", 0, 0, 0, 100, 93, 0, false, 15).score());  // mois terminé : pas de « trop vite »
    }

    @Test
    void lesNiveauxSuiventLesSeuils() {
        assertEquals(Niveau.FAIBLE, RisqueService.evaluer("1", 2, 0, 0, 100, 0, 0, true, 10).niveau());     // 24
        assertEquals(Niveau.MOYEN, RisqueService.evaluer("1", 0, 2, 0, 100, 0, 0, true, 10).niveau());      // 30
        assertEquals(Niveau.MOYEN, RisqueService.evaluer("1", 0, 1, 1, 100, 0, 0, true, 10).niveau());      // 40
        assertEquals(Niveau.ELEVE, RisqueService.evaluer("1", 3, 0, 1, 100, 0, 0, true, 10).niveau());      // 61
    }

    // ---------- l'API ----------

    @Test
    void lesPharmaciesSontClasseesDuPlusAuMoinsRisque() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution risquee = quota("risquee", m, 100);
        risquee.setAvertissements(2);
        risquee.setStatut(StatutAttribution.SUSPENDUE);
        attributions.save(risquee);
        refus("risquee", m, 3);
        quota("calme", m, 100);

        mvc.perform(get("/api/risques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pharmacieId").value("risquee"))
                .andExpect(jsonPath("$[0].score").value(91))
                .andExpect(jsonPath("$[0].niveau").value("ELEVE"))
                .andExpect(jsonPath("$[0].refus").value(3))
                .andExpect(jsonPath("$[0].avertissements").value(2))
                .andExpect(jsonPath("$[0].suspensions").value(1))
                .andExpect(jsonPath("$[0].raisons", hasItem("3 ventes refusées ce mois-ci")))
                .andExpect(jsonPath("$[1].pharmacieId").value("calme"))
                .andExpect(jsonPath("$[1].score").value(0))
                .andExpect(jsonPath("$[1].niveau").value("FAIBLE"))
                .andExpect(jsonPath("$[1].raisons", hasSize(0)));

        mvc.perform(get("/api/risques").param("aSurveiller", "true"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pharmacieId").value("risquee"));
        mvc.perform(get("/api/risques").param("mois", "2020-01"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void uneVenteDeFinDeMoisEstRepereeParLApi() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution a = quota("1", m, 100);
        a.ajouterVente(20);
        attributions.save(a);
        ventes.save(VenteSensible.builder().patientId("patient-1").ordonnanceId(1L).quantite(10)
                .date(YearMonth.now().atEndOfMonth().atTime(11, 0)).attribution(a).build());

        mvc.perform(get("/api/risques"))
                .andExpect(jsonPath("$[0].score").value(20))
                .andExpect(jsonPath("$[0].raisons[0]").value("50 % des boîtes vendues dans les 5 derniers jours du mois"));
    }

    @Test
    void deBoutEnBoutDeVraisRefusFontMonterLeScore() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 5);
        // deux ventes refusées (quota dépassé) : 2 avertissements -> suspension (RG3)
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 10)))
                    .andExpect(status().isConflict());
        }
        // 2 refus (24) + 2 avertissements (30) + suspension (25) = 79
        mvc.perform(get("/api/risques").param("aSurveiller", "true"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pharmacieId").value("1"))
                .andExpect(jsonPath("$[0].score").value(79))
                .andExpect(jsonPath("$[0].niveau").value("ELEVE"))
                .andExpect(jsonPath("$[0].raisons", hasSize(3)));
    }
}
