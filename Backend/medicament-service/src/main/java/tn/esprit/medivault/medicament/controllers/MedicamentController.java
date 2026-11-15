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

// CONTROLLER = le guichet de l'API : il reçoit les requêtes HTTP et renvoie les réponses.
// Il ne contient AUCUNE règle métier : il appelle le service.
// @RestController : chaque méthode renvoie du JSON.
// @RequestMapping("/api/medicaments") : toutes les URL de cette classe commencent par /api/medicaments.
// @RequiredArgsConstructor : Spring injecte medicamentService.
// @Tag / @Operation / @ApiResponse : textes affichés dans Swagger (documentation).
@RestController
@RequestMapping("/api/medicaments")
@RequiredArgsConstructor
@Tag(name = "Médicaments", description = "Catalogue des médicaments et vérification de compatibilité")
public class MedicamentController {

    // On dépend de l'INTERFACE, pas de MedicamentServiceImpl (couplage faible)
    private final IMedicamentService medicamentService;

    // GET /api/medicaments -> 200 + la liste en JSON
    @GetMapping
    @Operation(summary = "Consulter le catalogue des médicaments")
    public List<Medicament> getCatalogue() {
        return medicamentService.getCatalogue();
    }

    // POST /api/medicaments -> 201 Created
    // @RequestBody : le JSON du corps est transformé en MedicamentRequest.
    // @Valid : vérifie les règles du DTO (@NotBlank...) ; si erreur -> 400 automatiquement.
    @PostMapping
    @Operation(summary = "Ajouter un médicament au catalogue")
    @ApiResponse(responseCode = "201", description = "Médicament créé")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<Medicament> ajouter(@Valid @RequestBody MedicamentRequest request) {
        Medicament cree = medicamentService.ajouterMedicament(request);
        // Construit l'adresse du nouveau médicament : /api/medicaments/31
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(cree.getId()).toUri();
        // created(...) = statut 201 + en-tête Location + le médicament créé dans le corps
        return ResponseEntity.created(location).body(cree);
    }

    // POST /api/medicaments/compatibilite -> 200 + { compatible, alertes }
    // C'est un POST (et pas un GET) car on envoie une liste dans le corps.
    // URL avec un NOM ("compatibilite") et pas un verbe ("verifier") : règle REST du prof.
    @PostMapping("/compatibilite")
    @Operation(summary = "Vérifier la compatibilité d'une liste de médicaments avec les allergies du patient (RG4)")
    public VerificationResponse compatibilite(@Valid @RequestBody VerificationRequest request) {
        return medicamentService.verifierCompatibilite(request);
    }

    // DELETE /api/medicaments/5?motif=rappel -> 204 No Content (ou 404 si introuvable)
    // @PathVariable : prend le {id} dans l'URL.
    // @RequestParam(required = false) : ?motif=... facultatif.
    // @ResponseStatus(NO_CONTENT) : renvoie 204 (réussi, rien à renvoyer).
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
