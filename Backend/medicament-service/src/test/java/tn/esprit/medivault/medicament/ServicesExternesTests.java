package tn.esprit.medivault.medicament;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tn.esprit.medivault.medicament.clients.OrdonnanceClientFallbackFactory;
import tn.esprit.medivault.medicament.clients.UtilisateurClientFallbackFactory;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.exceptions.ObjetExterneIntrouvableException;
import tn.esprit.medivault.medicament.exceptions.ServiceIndisponibleException;
import tn.esprit.medivault.medicament.verification.VerificationExterne;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Panne d'ordonnance-service ou de user-service : "pas de vérification, pas de vente" (503), et aucun avertissement injuste.
class ServicesExternesTests extends ApiTestBase {

    @MockitoBean
    private VerificationExterne verification;

    @Test
    void ordonnanceServiceEnPanneRenvoie503() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        Attribution quota = quota("1", m, 50);
        when(verification.pharmacieActive("1")).thenReturn(true);
        when(verification.ordonnance("ORD-A7F3")).thenThrow(new ServiceIndisponibleException("ordonnance-service", null));

        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 2)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service indisponible"));
        assertEquals(0, ventes.count());
        assertEquals(0, attributions.findById(quota.getId()).orElseThrow().getAvertissements());
        assertEquals(0, attributions.findById(quota.getId()).orElseThrow().getQuantiteVendue());
    }

    @Test
    void userServiceEnPanneRenvoie503() throws Exception {
        MedicamentSensible m = medicament("Subutex 8 mg", Categorie.STUPEFIANT);
        quota("1", m, 50);
        when(verification.pharmacieActive("1")).thenThrow(new ServiceIndisponibleException("user-service", null));

        mvc.perform(post("/api/ventes").contentType(MediaType.APPLICATION_JSON).content(json("1", "ORD-A7F3", m.getId(), 2)))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, ventes.count());
    }

    // --- les "plans B" Feign : 404 = règle métier, tout le reste = panne ---

    private FeignException.NotFound introuvable() {
        Request requete = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("404", requete, null, Map.of());
    }

    @Test
    void fallbackOrdonnance() {
        var factory = new OrdonnanceClientFallbackFactory();
        assertThrows(ObjetExterneIntrouvableException.class, () -> factory.create(introuvable()).parCode("ORD-1"));
        assertThrows(ServiceIndisponibleException.class, () -> factory.create(new RuntimeException("délai dépassé")).parCode("ORD-1"));
    }

    @Test
    void fallbackUtilisateur() {
        var factory = new UtilisateurClientFallbackFactory();
        assertThrows(ObjetExterneIntrouvableException.class, () -> factory.create(introuvable()).parId("1"));
        assertThrows(ServiceIndisponibleException.class, () -> factory.create(new RuntimeException("circuit ouvert")).parId("1"));
    }
}
