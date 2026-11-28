package tn.esprit.medivault.soindomicile.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.medivault.soindomicile.dto.StatistiquesSoin;
import tn.esprit.medivault.soindomicile.services.IStatistiqueService;

/** Tableau de bord du back-office administrateur. */
@RestController
@RequestMapping("/api/visites/statistiques")
@RequiredArgsConstructor
public class StatistiqueController {

    private final IStatistiqueService service;

    @GetMapping
    public StatistiquesSoin statistiques() {
        return service.calculer();
    }
}
