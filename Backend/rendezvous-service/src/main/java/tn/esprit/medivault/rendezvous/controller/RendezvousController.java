package tn.esprit.medivault.rendezvous.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medivault.rendezvous.dto.RendezVousRequest;
import tn.esprit.medivault.rendezvous.dto.RendezVousResponse;
import tn.esprit.medivault.rendezvous.service.IRendezvousService;

import java.util.List;

@RestController
@RequestMapping("/api/rendezvous")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Prise et annulation de rendez-vous")
public class RendezvousController {

    private final IRendezvousService rendezvousService;

    @PostMapping
    @Operation(summary = "Prendre un rendez-vous (analyseId requis si au laboratoire)")
    public ResponseEntity<RendezVousResponse> prendreRendezVous(
            @Valid @RequestBody RendezVousRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId != null && "PATIENT".equalsIgnoreCase(userRole) && !userId.equalsIgnoreCase(request.getPatientId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès refusé : un patient ne peut réserver que pour son propre compte");
        }

        RendezVousResponse response = rendezvousService.prendreRendezVous(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister tous les rendez-vous")
    public ResponseEntity<List<RendezVousResponse>> getAllRendezVous() {
        List<RendezVousResponse> responses = rendezvousService.getAllRendezVous();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un rendez-vous par son identifiant")
    public ResponseEntity<RendezVousResponse> getRendezVousById(@PathVariable("id") Long id) {
        RendezVousResponse response = rendezvousService.getRendezVousById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les rendez-vous d'un patient")
    public ResponseEntity<List<RendezVousResponse>> getRendezVousByPatient(@PathVariable("patientId") String patientId) {
        List<RendezVousResponse> responses = rendezvousService.getRendezVousByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/professionnel/{professionnelId}")
    @Operation(summary = "Lister les rendez-vous d'un professionnel")
    public ResponseEntity<List<RendezVousResponse>> getRendezVousByProfessionnel(@PathVariable("professionnelId") String professionnelId) {
        List<RendezVousResponse> responses = rendezvousService.getRendezVousByProfessionnel(professionnelId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Annuler un rendez-vous (statut ANNULE et publication de RendezVousAnnule)")
    public ResponseEntity<RendezVousResponse> patchRendezVous(@PathVariable("id") Long id) {
        RendezVousResponse response = rendezvousService.annulerRendezVous(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler un rendez-vous (support PUT)")
    public ResponseEntity<RendezVousResponse> annulerRendezVous(@PathVariable("id") Long id) {
        RendezVousResponse response = rendezvousService.annulerRendezVous(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement un rendez-vous")
    public ResponseEntity<Void> supprimerRendezVous(@PathVariable("id") Long id) {
        rendezvousService.supprimerRendezVous(id);
        return ResponseEntity.noContent().build();
    }
}
