package tn.esprit.medivault.ordonnance.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.medivault.ordonnance.DTO.CompatibiliteRequestDTO;
import tn.esprit.medivault.ordonnance.DTO.CompatibiliteResponseDTO;

@FeignClient(name = "medicament-service", fallbackFactory = MedicamentClientFallbackFactory.class)
public interface MedicamentClient {

    @PostMapping("/api/medicaments/compatibilite")
    CompatibiliteResponseDTO verifierCompatibilite(@RequestBody CompatibiliteRequestDTO request);
}