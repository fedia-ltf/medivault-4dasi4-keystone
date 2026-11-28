package tn.esprit.medivault.rendezvous.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medivault.rendezvous.dto.CreneauRequest;
import tn.esprit.medivault.rendezvous.dto.CreneauResponse;
import tn.esprit.medivault.rendezvous.service.IRendezvousService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/creneaux")
@RequiredArgsConstructor
@Tag(name = "Créneaux", description = "Gestion et génération des créneaux de rendez-vous")
public class CreneauController {

    private final IRendezvousService rendezvousService;

    @PostMapping(path = {"/generer"})
    @Operation(summary = "Générer les créneaux d'un professionnel selon ses disponibilités")
    public ResponseEntity<List<CreneauResponse>> genererCreneaux(
            @RequestParam("professionnelId") String professionnelId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userRole != null && "PATIENT".equalsIgnoreCase(userRole)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès refusé : un patient ne peut pas générer de créneaux professionnels");
        }
        if (userId != null && !userId.equalsIgnoreCase(professionnelId) && !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès refusé : vous ne pouvez générer que vos propres créneaux");
        }

        List<CreneauResponse> creneaux = rendezvousService.genererCreneaux(professionnelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creneaux);
    }

    @PostMapping
    @Operation(summary = "Ajouter un créneau horaire ponctuel")
    public ResponseEntity<CreneauResponse> creerCreneau(
            @Valid @RequestBody CreneauRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userRole != null && "PATIENT".equalsIgnoreCase(userRole)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (userId != null && (request.getProfessionnelId() == null || request.getProfessionnelId().isBlank())) {
            request.setProfessionnelId(userId);
        }

        CreneauResponse response = rendezvousService.creerCreneau(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier l'horaire d'un créneau libre")
    public ResponseEntity<CreneauResponse> modifierCreneau(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreneauRequest request) {
        CreneauResponse response = rendezvousService.modifierCreneau(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Consulter les créneaux libres d'un professionnel")
    public ResponseEntity<List<CreneauResponse>> getCreneauxLibres(
            @RequestParam("professionnelId") String professionnelId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CreneauResponse> creneaux = rendezvousService.getCreneauxLibres(professionnelId, date);
        return ResponseEntity.ok(creneaux);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un créneau par son identifiant")
    public ResponseEntity<CreneauResponse> getCreneauById(@PathVariable("id") Long id) {
        CreneauResponse response = rendezvousService.getCreneauById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un créneau libre")
    public ResponseEntity<Void> supprimerCreneau(@PathVariable("id") Long id) {
        rendezvousService.supprimerCreneau(id);
        return ResponseEntity.noContent().build();
    }
}
