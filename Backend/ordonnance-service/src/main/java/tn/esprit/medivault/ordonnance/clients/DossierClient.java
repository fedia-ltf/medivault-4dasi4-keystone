package tn.esprit.medivault.ordonnance.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.ordonnance.DTO.AllergieResponseDTO;

@FeignClient(name = "dossier-service", fallbackFactory = DossierClientFallbackFactory.class)
public interface DossierClient {

    @GetMapping("/api/dossiers/patient/{patientId}/allergies")
    AllergieResponseDTO getAllergies(@PathVariable("patientId") Long patientId);
}