package tn.esprit.medivault.rendezvous.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.rendezvous.dto.DisponibilitesResponse;

/**
 * Appel synchrone (OpenFeign) vers user-service, résolu par Eureka.
 * Récupère les jours et heures de travail d'un professionnel de santé (médecin ou laborantin).
 * Si user-service est indisponible, le fallback refuse la génération des créneaux (503).
 */
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/professionnels/{id}/disponibilites")
    DisponibilitesResponse getDisponibilites(@PathVariable("id") String id);
}
