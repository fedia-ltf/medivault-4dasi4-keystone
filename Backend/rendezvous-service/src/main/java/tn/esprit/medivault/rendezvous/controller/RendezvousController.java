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

@RestController
@RequestMapping("/api/rendezvous")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Prise et annulation de rendez-vous")
public class RendezvousController {

    private final IRendezvousService rendezvousService;

    @PostMapping
    @Operation(summary = "Prendre un rendez-vous (analyseId requis si au laboratoire)")
    public ResponseEntity<RendezVousResponse> prendreRendezVous(@Valid @RequestBody RendezVousRequest request) {
        RendezVousResponse response = rendezvousService.prendreRendezVous(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
