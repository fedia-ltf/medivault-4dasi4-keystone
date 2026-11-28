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

    @GetMapping
    public List<OrdonnanceResponseDTO> trouverToutes() {
        return service.findAll();
    }

    @GetMapping("/patients/search")
    public List<String> rechercherPatients(@RequestParam(defaultValue = "") String q) {
        return service.findPatientIds(q);
    }

    @PutMapping("/{ordonnanceId}")
    public OrdonnanceResponseDTO modifier(@PathVariable Long ordonnanceId,
                                          @RequestBody OrdonnanceRequestDTO request) {
        return service.modifier(ordonnanceId, request);
    }

    @DeleteMapping("/{ordonnanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long ordonnanceId) {
        service.supprimer(ordonnanceId);
    }

    // Utilisé par soin-domicile-service (OpenFeign) : l'ordonnance d'un plan de soin
    @GetMapping("/{ordonnanceId}")
    public OrdonnanceResponseDTO trouverParId(@PathVariable Long ordonnanceId) {
        return service.findById(ordonnanceId);
    }

    @GetMapping("/code/{code}")
    public OrdonnanceResponseDTO trouverParCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping("/patient/{patientId}")
    public List<OrdonnanceResponseDTO> trouverParPatient(@PathVariable String patientId) {
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

    @PatchMapping("/{ordonnanceId}/statut")
    public OrdonnanceResponseDTO modifierStatut(@PathVariable Long ordonnanceId,
                                                  @RequestParam StatutOrdonnance statut) {
        return service.modifierStatut(ordonnanceId, statut);
    }
}
