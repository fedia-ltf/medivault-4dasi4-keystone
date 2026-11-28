package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.medicament.dto.AttributionDtos.*;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.services.AttributionService;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

// CRUD /attributions : les quotas mensuels des pharmacies (administrateur), + bilan, lot et nouveau mois.
@RestController
@RequestMapping("/api/attributions")
@RequiredArgsConstructor
@Tag(name = "Quotas (attributions)", description = "Quota mensuel de chaque pharmacie pour chaque médicament sensible")
public class AttributionController {

    private final AttributionService service;

    @GetMapping
    @Operation(summary = "Lister (filtres facultatifs : pharmacieId, medicamentId, mois = AAAA-MM, statut)")
    public List<Reponse> lister(@RequestParam(required = false) String pharmacieId,
                                @RequestParam(required = false) Long medicamentId,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                                @RequestParam(required = false) StatutAttribution statut) {
        return service.lister(pharmacieId, medicamentId, mois, statut);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un quota (404 s'il n'existe pas)")
    public Reponse obtenir(@PathVariable Long id) {
        return service.obtenir(id);
    }

    @PostMapping
    @Operation(summary = "Créer un quota (201, 400, 404 médicament inconnu, 409 quota déjà donné ce mois-ci)")
    public ResponseEntity<Reponse> creer(@Valid @RequestBody Requete requete) {
        Reponse cree = service.creer(requete);
        URI adresse = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(adresse).body(cree);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier la quantité ou le statut (200, 400, 404, 409 si inférieur aux boîtes déjà vendues)")
    public Reponse modifier(@PathVariable Long id, @Valid @RequestBody Modification modification) {
        return service.modifier(id, modification);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer (204, 404, 409 si des ventes existent)")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lot")
    @Operation(summary = "Même quota pour plusieurs pharmacies : renvoie les quotas créés et les pharmacies ignorées")
    public ResponseEntity<ResultatLot> lot(@Valid @RequestBody Lot lot) {
        return ResponseEntity.status(201).body(service.lot(lot));
    }

    @GetMapping("/{id}/bilan")
    @Operation(summary = "Bilan d'un quota : reçu, vendu, reste, avertissements")
    public Bilan bilan(@PathVariable Long id) {
        return service.bilan(id);
    }

    @PostMapping("/renouvellement")
    @Operation(summary = "RG4 : bilan du mois précédent + nouveaux quotas (mois = AAAA-MM, par défaut le mois courant). "
            + "Lancé automatiquement le 1er de chaque mois ; cette route permet de le déclencher à la main.")
    public Renouvellement renouveler(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return service.renouveler(mois != null ? mois : service.moisCourant());
    }
}