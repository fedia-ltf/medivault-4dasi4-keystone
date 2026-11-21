package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.medicament.dto.AttributionDtos.Disponibilite;
import tn.esprit.medivault.medicament.services.AttributionService;

import java.util.List;

// GET /disponibilites?medicamentId= : où trouver ce médicament ce mois-ci (médecin, patient : lecture seule).
@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
@Tag(name = "Disponibilités", description = "Pharmacies qui ont encore du quota pour un médicament")
public class DisponibiliteController {

    private final AttributionService service;

    @GetMapping
    @Operation(summary = "Disponibilité par pharmacie : DISPONIBLE, BIENTOT_EPUISE ou INDISPONIBLE (404 si médicament inconnu)")
    public List<Disponibilite> disponibilites(@RequestParam Long medicamentId) {
        return service.disponibilites(medicamentId);
    }
}