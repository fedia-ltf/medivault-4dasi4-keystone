package tn.esprit.medivault.medicament.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.medivault.medicament.dto.OrdonnanceDTO;

import java.util.List;

// CLIENT FEIGN = communication SYNCHRONE vers un autre microservice.
// On écrit seulement une interface ; Feign génère le code qui fait la requête HTTP.
// name = "ordonnance-service" : le nom inscrit dans Eureka -> Feign demande à Eureka
//   l'adresse réelle (pas besoin d'écrire localhost:8083).
// fallback = OrdonnanceClientFallback.class : le "plan B" (Resilience4j) si le service est en panne.
@FeignClient(name = "ordonnance-service", fallback = OrdonnanceClientFallback.class)
public interface OrdonnanceClient {

    // Appeler cette méthode = envoyer GET /api/ordonnances?medicamentId=5&statut=ACTIVE
    // à ordonnance-service, et recevoir la liste en JSON transformée en List<OrdonnanceDTO>.
    @GetMapping("/api/ordonnances")
    List<OrdonnanceDTO> getOrdonnances(@RequestParam("medicamentId") Long medicamentId,
                                       @RequestParam("statut") String statut);
}
