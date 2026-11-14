package tn.esprit.medivault.rendezvous.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping(path = {"/generer", ""})
    @Operation(summary = "Générer les créneaux d'un professionnel selon ses disponibilités")
    public ResponseEntity<List<CreneauResponse>> genererCreneaux(@RequestParam("professionnelId") String professionnelId) {
        List<CreneauResponse> creneaux = rendezvousService.genererCreneaux(professionnelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creneaux);
    }

    @GetMapping
    @Operation(summary = "Consulter les créneaux libres d'un professionnel")
    public ResponseEntity<List<CreneauResponse>> getCreneauxLibres(
            @RequestParam("professionnelId") String professionnelId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CreneauResponse> creneaux = rendezvousService.getCreneauxLibres(professionnelId, date);
        return ResponseEntity.ok(creneaux);
    }
}
