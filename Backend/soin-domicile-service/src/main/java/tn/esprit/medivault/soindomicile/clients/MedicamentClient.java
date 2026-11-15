package tn.esprit.medivault.soindomicile.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.medivault.soindomicile.dto.VenteRequest;

@FeignClient(name = "medicament-service", fallbackFactory = MedicamentClientFallback.class)
public interface MedicamentClient {
    @PostMapping("/api/ventes")
    void enregistrerVente(@RequestBody VenteRequest vente);
}
