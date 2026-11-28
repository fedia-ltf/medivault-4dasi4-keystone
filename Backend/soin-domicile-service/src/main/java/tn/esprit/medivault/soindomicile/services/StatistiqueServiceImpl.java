package tn.esprit.medivault.soindomicile.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.soindomicile.dto.StatistiquesSoin;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.repositories.InfirmierRepository;
import tn.esprit.medivault.soindomicile.repositories.PlanSuiviRepository;
import tn.esprit.medivault.soindomicile.repositories.VisiteRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calcule les chiffres du tableau de bord administrateur à partir de requêtes GROUP BY. */
@Service
@RequiredArgsConstructor
public class StatistiqueServiceImpl implements IStatistiqueService {

    private static final int TOP_INFIRMIERS = 5;

    private final VisiteRepository visiteRepo;
    private final InfirmierRepository infirmierRepo;
    private final PlanSuiviRepository planRepo;

    @Override
    @Transactional(readOnly = true)
    public StatistiquesSoin calculer() {
        Map<String, Long> parStatut = enMap(visiteRepo.compterParStatut());
        long realisees = parStatut.getOrDefault(StatutVisite.REALISEE.name(), 0L);
        long annulees = parStatut.getOrDefault(StatutVisite.ANNULEE.name(), 0L);
        long traitees = realisees + annulees;

        Map<String, Long> top = new LinkedHashMap<>();
        enMap(visiteRepo.compterSoinsRealisesParInfirmier()).entrySet().stream()
                .limit(TOP_INFIRMIERS)
                .forEach(e -> top.put(e.getKey(), e.getValue()));

        return new StatistiquesSoin(
                visiteRepo.count(),
                parStatut.getOrDefault(StatutVisite.DEMANDEE.name(), 0L),
                parStatut.getOrDefault(StatutVisite.ACCEPTEE.name(), 0L),
                realisees,
                annulees,
                traitees == 0 ? 0 : (int) Math.round(100.0 * realisees / traitees),
                infirmierRepo.count(),
                planRepo.count(),
                planRepo.countByDateFinGreaterThanEqual(LocalDate.now()),
                parStatut,
                enMap(visiteRepo.compterParType()),
                enMap(visiteRepo.compterParPharmacie()),
                enMap(infirmierRepo.compterParPharmacie()),
                top);
    }

    /** Transforme les lignes [clé, nombre] d'un GROUP BY en Map (l'ordre de la requête est conservé). */
    private static Map<String, Long> enMap(List<Object[]> lignes) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] l : lignes) {
            if (l[0] != null) m.put(String.valueOf(l[0]), ((Number) l[1]).longValue());
        }
        return m;
    }
}
