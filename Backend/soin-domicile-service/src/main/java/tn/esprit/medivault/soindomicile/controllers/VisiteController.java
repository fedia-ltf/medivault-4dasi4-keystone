package tn.esprit.medivault.soindomicile.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.entities.Visite;
import tn.esprit.medivault.soindomicile.services.IVisiteService;

import java.util.List;

@RestController
@RequestMapping("/api/visites")
@RequiredArgsConstructor
public class VisiteController {

    private final IVisiteService service;

    @PostMapping
    public ResponseEntity<Visite> demander(@Valid @RequestBody VisiteRequest r) {
        Visite v = service.demander(r);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(v.getId()).toUri()).body(v);
    }

    @GetMapping
    public List<Visite> lister(@RequestParam(required = false) String patientId,
                               @RequestParam(required = false) String pharmacieId,
                               @RequestParam(required = false) StatutVisite statut) {
        return service.lister(patientId, pharmacieId, statut);
    }

    @GetMapping("/{id}")
    public Visite getById(@PathVariable Long id) { return service.getById(id); }

    @PutMapping("/{id}")
    public Visite modifier(@PathVariable Long id, @Valid @RequestBody VisiteUpdateRequest r) {
        return service.modifier(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) { service.supprimer(id); }

    @PatchMapping("/{id}/accepter")
    public Visite accepter(@PathVariable Long id, @Valid @RequestBody AcceptationRequest r) {
        return service.accepter(id, r);
    }

    @PatchMapping("/{id}/rapport")
    public Visite rapport(@PathVariable Long id, @Valid @RequestBody RapportRequest r) {
        return service.saisirRapport(id, r);
    }

    @PatchMapping("/{id}/annuler")
    public Visite annuler(@PathVariable Long id) { return service.annuler(id); }
}
