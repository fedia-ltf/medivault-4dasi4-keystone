package tn.esprit.medivault.soindomicile.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.soindomicile.dto.InfirmierRequest;
import tn.esprit.medivault.soindomicile.entities.Infirmier;
import tn.esprit.medivault.soindomicile.services.IInfirmierService;

import java.util.List;

@RestController
@RequestMapping("/api/infirmiers")
@RequiredArgsConstructor
public class InfirmierController {

    private final IInfirmierService service;

    @GetMapping
    public List<Infirmier> lister(@RequestParam(required = false) String pharmacieId) {
        return service.lister(pharmacieId);
    }

    @GetMapping("/{id}")
    public Infirmier getById(@PathVariable Long id) { return service.getById(id); }

    @PostMapping
    public ResponseEntity<Infirmier> ajouter(@Valid @RequestBody InfirmierRequest r) {
        Infirmier i = service.ajouter(r);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(i.getId()).toUri()).body(i);
    }

    @PutMapping("/{id}")
    public Infirmier modifier(@PathVariable Long id, @Valid @RequestBody InfirmierRequest r) {
        return service.modifier(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) { service.supprimer(id); }
}
