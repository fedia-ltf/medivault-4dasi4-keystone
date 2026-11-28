package tn.esprit.medivault.soindomicile.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.PlanSuivi;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import tn.esprit.medivault.soindomicile.entities.Visite;
import tn.esprit.medivault.soindomicile.services.IPlanSuiviService;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanSuiviController {

    private final IPlanSuiviService service;

    @PostMapping
    public ResponseEntity<PlanSuivi> creer(@Valid @RequestBody PlanSuiviRequest r) {
        PlanSuivi p = service.creer(r);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(p.getId()).toUri()).body(p);
    }

    @GetMapping
    public List<PlanSuivi> lister(@RequestParam(required = false) String patientId,
                                  @RequestParam(required = false) String medecinId) {
        return service.lister(patientId, medecinId);
    }

    /** Recherche : /api/plans/recherche?medecinId=...&q=patient&typeSoin=GLYCEMIE&enCours=true */
    @GetMapping("/recherche")
    public List<PlanSuivi> rechercher(@RequestParam(required = false) String medecinId,
                                      @RequestParam(required = false) String patientId,
                                      @RequestParam(required = false) TypeSoin typeSoin,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Boolean enCours) {
        return service.rechercher(medecinId, patientId, typeSoin, q, enCours);
    }

    @GetMapping("/{id}")
    public PlanSuivi getById(@PathVariable Long id) { return service.getById(id); }

    @GetMapping("/{id}/visites")
    public List<Visite> visites(@PathVariable Long id) { return service.getVisites(id); }

    @PutMapping("/{id}")
    public PlanSuivi modifier(@PathVariable Long id, @Valid @RequestBody PlanSuiviUpdateRequest r) {
        return service.modifier(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arreter(@PathVariable Long id) { service.arreter(id); }
}
