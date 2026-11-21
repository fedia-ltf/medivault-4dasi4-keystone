package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.medicament.dto.RisqueDtos.Pharmacie;
import tn.esprit.medivault.medicament.services.RisqueService;

import java.time.YearMonth;
import java.util.List;

// GET /risques : les pharmacies à surveiller (administrateur, lecture seule).
@RestController
@RequestMapping("/api/risques")
@RequiredArgsConstructor
@Tag(name = "Risque des pharmacies", description = "Score de risque expliqué (0 à 100) : refus, avertissements, suspension, ventes de fin de mois")
public class RisqueController {

    private final RisqueService service;

    @GetMapping
    @Operation(summary = "Score de risque par pharmacie, la plus à risque d'abord (mois AAAA-MM, mois courant par défaut ; aSurveiller=true : seulement MOYEN et ELEVE)")
    public List<Pharmacie> pharmacies(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                                      @RequestParam(defaultValue = "false") boolean aSurveiller) {
        return service.pharmacies(mois, aSurveiller);
    }
}
