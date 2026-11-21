package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.medicament.dto.VenteDtos.Reponse;
import tn.esprit.medivault.medicament.dto.VenteDtos.Requete;
import tn.esprit.medivault.medicament.services.ResultatVente;
import tn.esprit.medivault.medicament.services.VenteService;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

// /ventes : déclarer une vente (pharmacien, ou soin-domicile pour une injection) et consulter l'historique (admin).
// Une vente n'est jamais modifiée ni supprimée : il n'y a donc ni PUT ni DELETE.
@RestController
@RequestMapping("/api/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Déclaration des ventes de médicaments sensibles et historique")
public class VenteController {

    private final VenteService service;

    @PostMapping
    @Operation(summary = "Déclarer une vente : 201 enregistrée · 403 pharmacie inactive, suspendue ou sans quota · "
            + "409 ordonnance non valable (RG1) ou quota épuisé (RG2), avec avertissement (RG3) · 404 médicament inconnu · 503 service externe en panne")
    public ResponseEntity<?> declarer(@Valid @RequestBody Requete requete) {
        ResultatVente resultat = service.declarer(requete);
        if (resultat.estAccepte()) {
            URI adresse = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(resultat.vente().id()).toUri();
            return ResponseEntity.created(adresse).body(resultat.vente());
        }
        // Refus : réponse d'erreur expliquée, avec l'état des avertissements de la pharmacie
        ResultatVente.Refus refus = resultat.refus();
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(refus.statut(), refus.motif());
        probleme.setTitle(refus.titre());
        probleme.setProperty("avertissements", refus.avertissements());
        probleme.setProperty("suspendue", refus.suspendue());
        return ResponseEntity.status(refus.statut()).body(probleme);
    }

    @GetMapping
    @Operation(summary = "Historique des ventes (filtres facultatifs : pharmacieId, medicamentId, mois = AAAA-MM)")
    public List<Reponse> lister(@RequestParam(required = false) String pharmacieId,
                                @RequestParam(required = false) Long medicamentId,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return service.lister(pharmacieId, medicamentId, mois);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Une vente (404 si elle n'existe pas)")
    public Reponse obtenir(@PathVariable Long id) {
        return service.obtenir(id);
    }
}