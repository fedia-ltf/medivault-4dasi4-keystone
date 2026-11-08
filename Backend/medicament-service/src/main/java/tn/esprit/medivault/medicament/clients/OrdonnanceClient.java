package tn.esprit.medivault.medicament.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.medivault.medicament.dto.OrdonnanceDTO;

import java.util.List;

// Client Feign vers ordonnance-service (résolu via Eureka), protégé par un circuit breaker
@FeignClient(name = "ordonnance-service", fallback = OrdonnanceClientFallback.class)
public interface OrdonnanceClient {

    @GetMapping("/api/ordonnances")
    List<OrdonnanceDTO> getOrdonnances(@RequestParam("medicamentId") Long medicamentId,
                                       @RequestParam("statut") String statut);
}
