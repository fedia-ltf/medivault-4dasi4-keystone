package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.medicament.dto.MedicamentRequest;
import tn.esprit.medivault.medicament.dto.VerificationRequest;
import tn.esprit.medivault.medicament.dto.VerificationResponse;
import tn.esprit.medivault.medicament.entities.Medicament;
import tn.esprit.medivault.medicament.services.IMedicamentService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/medicaments")
@RequiredArgsConstructor
@Tag(name = "Médicaments", description = "Catalogue des médicaments et vérification de compatibilité")
public class MedicamentController {

    private final IMedicamentService medicamentService;

    @GetMapping
    @Operation(summary = "Consulter le catalogue des médicaments")
    public List<Medicament> getCatalogue() {
        return medicamentService.getCatalogue();
    }

    @PostMapping
    @Operation(summary = "Ajouter un médicament au catalogue")
    @ApiResponse(responseCode = "201", description = "Médicament créé")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<Medicament> ajouter(@Valid @RequestBody MedicamentRequest request) {
        Medicament cree = medicamentService.ajouterMedicament(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(cree.getId()).toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @PostMapping("/compatibilite")
    @Operation(summary = "Vérifier la compatibilité d'une liste de médicaments avec les allergies du patient (RG4)")
    public VerificationResponse compatibilite(@Valid @RequestBody VerificationRequest request) {
        return medicamentService.verifierCompatibilite(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirer un médicament du catalogue (RG6)")
    @ApiResponse(responseCode = "204", description = "Médicament retiré")
    @ApiResponse(responseCode = "404", description = "Médicament introuvable")
    public void retirer(@PathVariable Long id,
                        @RequestParam(required = false) String motif) {
        medicamentService.retirerMedicament(id, motif);
    }
}
