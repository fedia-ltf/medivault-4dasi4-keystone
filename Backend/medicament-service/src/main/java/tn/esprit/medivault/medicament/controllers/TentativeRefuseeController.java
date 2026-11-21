package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.medicament.dto.TentativeDtos.Reponse;
import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.services.TentativeRefuseeService;

import java.time.YearMonth;
import java.util.List;

// GET /tentatives-refusees : l'historique des ventes refusées (administrateur, lecture seule).
// Il se remplit tout seul : chaque refus de POST /ventes y laisse une trace (qui, quoi, quand, pourquoi).
@RestController
@RequestMapping("/api/tentatives-refusees")
@RequiredArgsConstructor
@Tag(name = "Historique des refus", description = "Chaque tentative de vente refusée : pharmacie, médicament, ordonnance, motif, date")
public class TentativeRefuseeController {

    private final TentativeRefuseeService service;

    @GetMapping
    @Operation(summary = "Lister les ventes refusées, les plus récentes d'abord (filtres : pharmacieId, medicamentId, mois AAAA-MM, motif)")
    public List<Reponse> lister(@RequestParam(required = false) String pharmacieId,
                                @RequestParam(required = false) Long medicamentId,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                                @RequestParam(required = false) MotifRefus motif) {
        return service.lister(pharmacieId, medicamentId, mois, motif);
    }
}
