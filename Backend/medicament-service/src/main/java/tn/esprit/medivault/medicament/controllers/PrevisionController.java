package tn.esprit.medivault.medicament.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.medicament.dto.PrevisionDtos;
import tn.esprit.medivault.medicament.services.PrevisionService;

import java.time.YearMonth;
import java.util.List;

// GET /previsions : « à ce rythme, le quota sera épuisé vers le 23 » (lecture seule).
@RestController
@RequestMapping("/api/previsions")
@RequiredArgsConstructor
@Tag(name = "Prévisions", description = "Prévision d'épuisement des quotas et alerte à 80 %")
public class PrevisionController {

    private final PrevisionService service;

    @GetMapping
    @Operation(summary = "Une prévision par quota (filtres : pharmacieId, medicamentId, mois AAAA-MM)")
    public List<PrevisionDtos.Quota> parQuota(@RequestParam(required = false) String pharmacieId,
                                              @RequestParam(required = false) Long medicamentId,
                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return service.parQuota(pharmacieId, medicamentId, mois);
    }

    @GetMapping("/medicaments")
    @Operation(summary = "Une prévision par médicament, toutes pharmacies confondues (mois AAAA-MM)")
    public List<PrevisionDtos.Medicament> parMedicament(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return service.parMedicament(mois);
    }
}
