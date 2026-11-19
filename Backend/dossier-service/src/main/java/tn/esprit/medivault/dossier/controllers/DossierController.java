package tn.esprit.medivault.dossier.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medivault.dossier.dto.*;
import tn.esprit.medivault.dossier.services.DossierService;

@RestController
@RequestMapping("/dossiers")
@RequiredArgsConstructor
@Tag(name = "Dossiers")
public class DossierController {

    private final DossierService dossierService;

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Récupérer le dossier complet d'un patient")
    @ApiResponse(responseCode = "200", description = "Dossier trouvé")
    @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    public ResponseEntity<DossierResponse> getDossier(@PathVariable String patientId) {
        DossierResponse resp = dossierService.getDossierByPatientId(patientId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/consultations")
    @Operation(summary = "Ajouter une consultation au dossier")
    @ApiResponse(responseCode = "201", description = "Consultation créée")
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    public ResponseEntity<ConsultationResponse> addConsultation(
            @PathVariable Long id,
            @Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse resp = dossierService.ajouterConsultation(id, request);
        return ResponseEntity.status(201).body(resp);
    }

    @PutMapping("/{id}/consultations/{consultationId}")
    @Operation(summary = "Modifier une consultation existante")
    @ApiResponse(responseCode = "200", description = "Consultation modifiée")
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Dossier ou consultation introuvable")
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable Long id,
            @PathVariable Long consultationId,
            @Valid @RequestBody ConsultationUpdateRequest request) {
        ConsultationResponse resp = dossierService.modifierConsultation(id, consultationId, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}/consultations/{consultationId}")
    @Operation(summary = "Supprimer une consultation du dossier")
    @ApiResponse(responseCode = "204", description = "Consultation supprimée")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "404", description = "Dossier ou consultation introuvable")
    public ResponseEntity<Void> deleteConsultation(
            @PathVariable Long id,
            @PathVariable Long consultationId,
            @RequestParam String medecinId) {
        dossierService.supprimerConsultation(id, consultationId, medecinId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/{patientId}/allergies")
    @Operation(summary = "Obtenir les allergies d'un patient")
    @ApiResponse(responseCode = "200", description = "Allergies retournées")
    @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    public ResponseEntity<AllergiesResponse> getAllergies(@PathVariable String patientId) {
        AllergiesResponse resp = dossierService.getAllergies(patientId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/patient/{patientId}/acces")
    @Operation(summary = "Vérifier l'accès d'un professionnel au dossier d'un patient")
    @ApiResponse(responseCode = "200", description = "Justification d'accès retournée")
    @ApiResponse(responseCode = "404", description = "Dossier introuvable")
    public ResponseEntity<AccesJustificationResponse> verifierAcces(
            @PathVariable String patientId,
            @RequestParam String professionnelId) {
        AccesJustificationResponse resp = dossierService.verifierAcces(patientId, professionnelId);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{id}/acces/{professionnelId}")
    @Operation(summary = "Retirer l'accès d'un professionnel au dossier")
    @ApiResponse(responseCode = "200", description = "Accès retiré")
    @ApiResponse(responseCode = "404", description = "Dossier ou accès introuvable")
    public ResponseEntity<AccesResponse> retirerAcces(
            @PathVariable Long id,
            @PathVariable String professionnelId) {
        AccesResponse resp = dossierService.retirerAcces(id, professionnelId);
        return ResponseEntity.ok(resp);
    }
}
