package tn.esprit.medivault.rendezvous.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.rendezvous.dto.AnalyseResponse;

/**
 * Appel synchrone (OpenFeign) vers analyse-service, résolu par Eureka.
 * Lecture de la prescription pour valider un rendez-vous au laboratoire :
 * existence de l'analyse, correspondance du patient et statut EN_ATTENTE.
 */
@FeignClient(name = "analyse-service", fallback = AnalyseClientFallback.class)
public interface AnalyseClient {

    @GetMapping("/api/analyses/{id}")
    AnalyseResponse getAnalyseById(@PathVariable("id") Long id);
}

