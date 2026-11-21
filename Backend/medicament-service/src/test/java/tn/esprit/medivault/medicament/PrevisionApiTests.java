package tn.esprit.medivault.medicament;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import tn.esprit.medivault.medicament.dto.PrevisionDtos.Niveau;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.events.QuotaBientotEpuise;
import tn.esprit.medivault.medicament.services.PrevisionService;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// La prévision d'épuisement (le calcul, puis l'API) et l'alerte à 80 % publiée sur RabbitMQ.
class PrevisionApiTests extends ApiTestBase {

    private static final YearMonth NOVEMBRE = YearMonth.of(2026, 11);

    // ---------- le calcul seul (fonction pure : dates choisies, résultat toujours le même) ----------

    @Test
    void aCeRythmeLeQuotaSeraEpuiseLe20() {
        // 50 boîtes vendues en 10 jours = 5 par jour ; il en reste 50 : encore 10 jours
        var c = PrevisionService.calculer(100, 50, NOVEMBRE, LocalDate.of(2026, 11, 10));
        assertEquals(5.0, c.rythme());
        assertEquals(LocalDate.of(2026, 11, 20), c.dateEpuisement());
        assertTrue(c.avantFinDeMois());
        assertEquals(Niveau.A_SURVEILLER, c.niveau());
        assertEquals("À ce rythme, le quota sera épuisé vers le 20 novembre.", c.message());
    }

    @Test
    void siLeRythmeSuffitOnLeDit() {
        // 10 boîtes en 10 jours : il faudrait 90 jours pour finir le quota
        var c = PrevisionService.calculer(100, 10, NOVEMBRE, LocalDate.of(2026, 11, 10));
        assertNull(c.dateEpuisement());
        assertFalse(c.avantFinDeMois());
        assertEquals(Niveau.OK, c.niveau());
        assertEquals("À ce rythme, le quota suffira jusqu'à la fin du mois.", c.message());
    }

    @Test
    void l80PourCentDeclencheLAlerte() {
        assertEquals(Niveau.ALERTE, PrevisionService.calculer(100, 80, NOVEMBRE, LocalDate.of(2026, 11, 10)).niveau());
        assertNotEquals(Niveau.ALERTE, PrevisionService.calculer(100, 79, NOVEMBRE, LocalDate.of(2026, 11, 10)).niveau());
        var c = PrevisionService.calculer(100, 85, NOVEMBRE, LocalDate.of(2026, 11, 10));
        assertTrue(c.message().startsWith("Attention : 85 % du quota est déjà utilisé."));
    }

    @Test
    void unQuotaVideEstEpuise() {
        var c = PrevisionService.calculer(50, 50, NOVEMBRE, LocalDate.of(2026, 11, 10));
        assertEquals(Niveau.EPUISE, c.niveau());
        assertEquals(0, c.reste());
        assertNull(c.dateEpuisement());
    }

    @Test
    void sansVenteAucunEpuisementPrevu() {
        var c = PrevisionService.calculer(100, 0, NOVEMBRE, LocalDate.of(2026, 11, 10));
        assertEquals(Niveau.OK, c.niveau());
        assertEquals(0.0, c.rythme());
        assertNull(c.dateEpuisement());
    }

    @Test
    void unMoisPasseOuFuturNEstPasPrevu() {
        var passe = PrevisionService.calculer(100, 60, YearMonth.of(2026, 10), LocalDate.of(2026, 11, 10));
        assertEquals("Mois terminé : 60 % du quota utilisé.", passe.message());
        assertNull(passe.dateEpuisement());
        var futur = PrevisionService.calculer(100, 0, YearMonth.of(2026, 12), LocalDate.of(2026, 11, 10));
        assertEquals("Mois à venir : aucune vente pour l'instant.", futur.message());
    }

    // ---------- l'API ----------

    @Test
    void lApiDonneUnePrevisionParQuota() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution a = quota("1", m, 100);
        a.ajouterVente(85);
        attributions.save(a);
        quota("2", m, 100);

        mvc.perform(get("/api/previsions").param("medicamentId", m.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pharmacieId").value("1"))
                .andExpect(jsonPath("$[0].medicamentNom").value("Subutex 8 mg"))
                .andExpect(jsonPath("$[0].quantiteAttribuee").value(100))
                .andExpect(jsonPath("$[0].quantiteVendue").value(85))
                .andExpect(jsonPath("$[0].reste").value(15))
                .andExpect(jsonPath("$[0].pourcentageUtilise").value(85))
                .andExpect(jsonPath("$[0].niveau").value("ALERTE"))
                .andExpect(jsonPath("$[1].pharmacieId").value("2"))
                .andExpect(jsonPath("$[1].niveau").value("OK"))
                .andExpect(jsonPath("$[1].dateEpuisementPrevue").doesNotExist());
        mvc.perform(get("/api/previsions").param("pharmacieId", "2"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/previsions").param("mois", "2020-01"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void lApiDonneUnePrevisionParMedicament() throws Exception {
        MedicamentSensible subutex = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        MedicamentSensible xanax = medicament("Xanax 0,5 mg", Categorie.PSYCHOTROPE);
        Attribution a = quota("1", subutex, 100);
        a.ajouterVente(90);
        attributions.save(a);
        quota("2", subutex, 100);
        quota("1", xanax, 40);

        mvc.perform(get("/api/previsions/medicaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].medicamentNom").value("Subutex 8 mg"))
                .andExpect(jsonPath("$[0].pharmacies").value(2))
                .andExpect(jsonPath("$[0].quantiteAttribuee").value(200))
                .andExpect(jsonPath("$[0].quantiteVendue").value(90))
                .andExpect(jsonPath("$[0].reste").value(110))
                .andExpect(jsonPath("$[0].pourcentageUtilise").value(45))
                .andExpect(jsonPath("$[0].pharmaciesEnAlerte").value(1))
                .andExpect(jsonPath("$[1].medicamentNom").value("Xanax 0,5 mg"))
                .andExpect(jsonPath("$[1].pharmaciesEnAlerte").value(0));
    }

    // ---------- l'alerte à 80 % ----------

    @Test
    void l80PourCentPubliqueUneSeuleAlerte() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 10);

        for (int quantite : new int[]{7, 1, 1}) {      // 70 % : rien ; 80 % : ALERTE ; 90 % : déjà signalé
            mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), quantite)))
                    .andExpect(status().isCreated());
        }

        ArgumentCaptor<Object> evenement = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("medivault.events"), eq("quota.bientot.epuise"), evenement.capture());
        QuotaBientotEpuise alerte = (QuotaBientotEpuise) evenement.getValue();
        assertEquals("1", alerte.pharmacieId());
        assertEquals(m.getId(), alerte.medicamentId());
        assertEquals("Subutex 8 mg", alerte.medicamentNom());
        assertEquals(80, alerte.pourcentageUtilise());
        assertEquals(2, alerte.reste());
        verify(rabbitTemplate, times(3)).convertAndSend(eq("medivault.events"), eq("vente.sensible.enregistree"), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void sousLeSeuilAucuneAlerte() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 100);
        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 10)))
                .andExpect(status().isCreated());
        verify(rabbitTemplate, never()).convertAndSend(eq("medivault.events"), eq("quota.bientot.epuise"), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
