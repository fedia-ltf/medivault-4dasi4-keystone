package tn.esprit.medivault.soindomicile.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.soindomicile.dto.OrdonnanceDTO;

@FeignClient(name = "ordonnance-service", fallbackFactory = OrdonnanceClientFallback.class)
public interface OrdonnanceClient {
    @GetMapping("/api/ordonnances/{id}")
    OrdonnanceDTO getById(@PathVariable("id") Long id);
}
