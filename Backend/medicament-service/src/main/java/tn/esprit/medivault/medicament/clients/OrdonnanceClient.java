package tn.esprit.medivault.medicament.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.medicament.dto.ExterneDtos;

// CLIENT FEIGN = communication SYNCHRONE vers un autre microservice (documentation : "Synchrone 1").
// On écrit seulement une interface ; Feign génère le code qui fait la requête HTTP.
// name = "ordonnance-service" : le nom inscrit dans Eureka -> Feign demande à Eureka l'adresse réelle.
// fallbackFactory : le "plan B" (Resilience4j) quand le service est en panne : ici, la vente est refusée (503).
@FeignClient(name = "ordonnance-service", fallbackFactory = OrdonnanceClientFallbackFactory.class)
public interface OrdonnanceClient {

    // GET /api/ordonnances/code/ORD-2026-0123 : l'ordonnance avec son statut et ses lignes (médicament, quantité)
    @GetMapping("/api/ordonnances/code/{code}")
    ExterneDtos.Ordonnance parCode(@PathVariable("code") String code);
}
