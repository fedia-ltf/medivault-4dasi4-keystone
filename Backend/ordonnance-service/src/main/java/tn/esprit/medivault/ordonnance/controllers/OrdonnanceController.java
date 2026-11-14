package tn.esprit.medivault.ordonnance.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medivault.ordonnance.dto.OrdonnanceRequestDTO;
import tn.esprit.medivault.ordonnance.dto.OrdonnanceResponseDTO;
import tn.esprit.medivault.ordonnance.entity.StatutOrdonnance;
import tn.esprit.medivault.ordonnance.services.OrdonnanceService;

import java.util.List;

@RestController
@RequestMapping("/api/ordonnances")
public class OrdonnanceController {
    private final OrdonnanceService service;

    public OrdonnanceController(OrdonnanceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdonnanceResponseDTO creer(@RequestBody OrdonnanceRequestDTO request) {
        return service.creer(request);
    }

    @GetMapping("/code/{code}")
    public OrdonnanceResponseDTO trouverParCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping("/patient/{patientId}")
    public List<OrdonnanceResponseDTO> trouverParPatient(@PathVariable Long patientId) {
        return service.findByPatient(patientId);
    }

    @GetMapping("/medicament/{medicamentId}")
    public List<OrdonnanceResponseDTO> trouverParMedicament(
            @PathVariable Long medicamentId,
            @RequestParam StatutOrdonnance statut) {
        return service.findByMedicamentEtStatut(medicamentId, statut);
    }

    @PatchMapping("/{ordonnanceId}/delivree")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marquerDelivree(@PathVariable Long ordonnanceId) {
        service.marquerDelivree(ordonnanceId);
    }
}
