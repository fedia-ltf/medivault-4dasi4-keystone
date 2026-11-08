package tn.esprit.medivault.analyse.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.medivault.analyse.dto.AccesResponse;

/**
 * Appel synchrone (OpenFeign) vers dossier-service, résolu par Eureka.
 * Avant la saisie d'un résultat : le laboratoire a-t-il un accès ACTIF au dossier du patient ?
 * Si dossier-service est indisponible, le fallback refuse la saisie (fail-closed, 503).
 */
@FeignClient(name = "dossier-service", fallback = DossierClientFallback.class)
public interface DossierClient {

    @GetMapping("/api/dossiers/patient/{patientId}/acces")
    AccesResponse verifierAcces(@PathVariable("patientId") Long patientId,
                                @RequestParam("professionnelId") Long professionnelId);
}
