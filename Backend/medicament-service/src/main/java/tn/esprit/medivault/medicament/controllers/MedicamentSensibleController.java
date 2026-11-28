package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.medicament.dto.MedicamentSensibleDtos.Reponse;
import tn.esprit.medivault.medicament.dto.MedicamentSensibleDtos.Requete;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.services.MedicamentSensibleService;

import java.net.URI;
import java.util.List;

// CRUD /medicaments-sensibles : la liste des médicaments sous contrôle (administrateur).
// Le controller ne contient aucune logique : il reçoit la requête, appelle le service, renvoie la réponse.
@RestController
@RequestMapping("/api/medicaments-sensibles")
@RequiredArgsConstructor
@Tag(name = "Médicaments sensibles", description = "CRUD de la liste des médicaments sous contrôle")
public class MedicamentSensibleController {

    private final MedicamentSensibleService service;

    @GetMapping
    @Operation(summary = "Lister (filtres facultatifs : categorie, q = nom ou DCI)")
    public List<Reponse> lister(@RequestParam(required = false) Categorie categorie,
                                @RequestParam(name = "q", required = false) String recherche) {
        return service.lister(categorie, recherche);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un médicament sensible (404 s'il n'existe pas)")
    public Reponse obtenir(@PathVariable Long id) {
        return service.obtenir(id);
    }

    @PostMapping
    @Operation(summary = "Créer (201, 400 données invalides, 409 le médicament existe déjà)")
    public ResponseEntity<Reponse> creer(@Valid @RequestBody Requete requete) {
        Reponse cree = service.creer(requete);
        URI adresse = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(adresse).body(cree);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier (200, 400, 404, 409)")
    public Reponse modifier(@PathVariable Long id, @Valid @RequestBody Requete requete) {
        return service.modifier(id, requete);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer (204, 404, 409 s'il a encore des quotas)")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}