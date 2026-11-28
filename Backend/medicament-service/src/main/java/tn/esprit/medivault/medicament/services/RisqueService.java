package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.RisqueDtos;
import tn.esprit.medivault.medicament.dto.RisqueDtos.Niveau;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;
import tn.esprit.medivault.medicament.entities.VenteSensible;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.TentativeRefuseeRepository;
import tn.esprit.medivault.medicament.repositories.VenteSensibleRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

// SCORE DE RISQUE d'une pharmacie pour un mois, de 0 à 100. Aucune boîte noire : le score est une somme de
// points expliqués, et chaque point est renvoyé sous forme de phrase (« raisons »).
//
//   refus de vente ........................ 12 points par refus (maximum 40)
//   avertissements (RG3) .................. 15 points par avertissement (maximum 30)
//   quota suspendu ........................ 25 points
//   ventes concentrées en fin de mois ..... 20 points si 40 % ou plus des boîtes sont vendues dans les 5 derniers jours
//                                           (au moins 10 boîtes vendues)
//   quota consommé trop vite .............. 10 points si 90 % du quota est parti avant le 20 du mois
//
//   0-29 FAIBLE · 30-59 MOYEN (à surveiller) · 60 et plus ELEVE (à surveiller de près)
@Service
@RequiredArgsConstructor
public class RisqueService {

    private static final int JOURS_FIN_DE_MOIS = 5;

    private final AttributionRepository attributions;
    private final VenteSensibleRepository ventes;
    private final TentativeRefuseeRepository tentatives;
    private final Clock clock;

    // Ce qu'on sait d'une pharmacie sur le mois (rempli puis transformé en score)
    private static final class Dossier {
        int refus, avertissements, suspensions, attribue, vendu, vendusFinDeMois;
    }

    // Le calcul du score, séparé pour pouvoir être testé sans base de données
    public static RisqueDtos.Pharmacie evaluer(String pharmacieId, int refus, int avertissements, int suspensions,
                                        int attribue, int vendu, int vendusFinDeMois, boolean moisEnCours, int jourDuMois) {
        int tauxConsommation = attribue > 0 ? (int) (vendu * 100L / attribue) : 0;
        List<String> raisons = new ArrayList<>();
        int score = 0;

        if (refus > 0) {
            score += Math.min(40, refus * 12);
            raisons.add(refus + (refus > 1 ? " ventes refusées" : " vente refusée") + " ce mois-ci");
        }
        if (avertissements > 0) {
            score += Math.min(30, avertissements * 15);
            raisons.add(avertissements + (avertissements > 1 ? " avertissements" : " avertissement"));
        }
        if (suspensions > 0) {
            score += 25;
            raisons.add("Suspendue pour " + suspensions + (suspensions > 1 ? " médicaments" : " médicament"));
        }
        if (vendu >= 10 && vendusFinDeMois * 100L >= 40L * vendu) {
            score += 20;
            raisons.add((int) (vendusFinDeMois * 100L / vendu) + " % des boîtes vendues dans les " + JOURS_FIN_DE_MOIS + " derniers jours du mois");
        }
        if (moisEnCours && jourDuMois <= 20 && attribue > 0 && tauxConsommation >= 90) {
            score += 10;
            raisons.add("Quota consommé à " + tauxConsommation + " % avant le 20 du mois");
        }
        score = Math.min(100, score);
        Niveau niveau = score >= 60 ? Niveau.ELEVE : score >= 30 ? Niveau.MOYEN : Niveau.FAIBLE;
        return new RisqueDtos.Pharmacie(pharmacieId, score, niveau, refus, avertissements, suspensions, tauxConsommation, raisons);
    }

    // GET /risques : les pharmacies du mois, la plus à risque d'abord. aSurveillerSeulement = niveau MOYEN ou ELEVE.
    @Transactional(readOnly = true)
    public List<RisqueDtos.Pharmacie> pharmacies(YearMonth mois, boolean aSurveillerSeulement) {
        YearMonth m = mois != null ? mois : YearMonth.now(clock);
        LocalDate aujourdhui = LocalDate.now(clock);
        boolean enCours = m.equals(YearMonth.from(aujourdhui));
        Map<String, Dossier> dossiers = new TreeMap<>();

        for (Attribution a : attributions.findByMois(m)) {
            Dossier d = dossiers.computeIfAbsent(a.getPharmacieId(), k -> new Dossier());
            d.avertissements += a.getAvertissements();
            d.attribue += a.getQuantiteAttribuee();
            d.vendu += a.getQuantiteVendue();
            if (a.getStatut() == StatutAttribution.SUSPENDUE) {
                d.suspensions++;
            }
        }
        int premierJourDeFin = m.lengthOfMonth() - JOURS_FIN_DE_MOIS + 1;
        for (VenteSensible v : ventes.findAll((r, q, cb) -> cb.equal(r.get("attribution").get("mois"), m))) {
            if (v.getDate().getDayOfMonth() >= premierJourDeFin) {
                dossiers.computeIfAbsent(v.getAttribution().getPharmacieId(), k -> new Dossier()).vendusFinDeMois += v.getQuantite();
            }
        }
        for (TentativeRefusee t : tentatives.findByDateGreaterThanEqualAndDateLessThan(m.atDay(1).atStartOfDay(), m.plusMonths(1).atDay(1).atStartOfDay())) {
            dossiers.computeIfAbsent(t.getPharmacieId(), k -> new Dossier()).refus++;
        }

        return dossiers.entrySet().stream()
                .map(e -> {
                    Dossier d = e.getValue();
                    return evaluer(e.getKey(), d.refus, d.avertissements, d.suspensions, d.attribue, d.vendu, d.vendusFinDeMois,
                            enCours, aujourdhui.getDayOfMonth());
                })
                .filter(r -> !aSurveillerSeulement || r.niveau() != Niveau.FAIBLE)
                .sorted(Comparator.comparingInt(RisqueDtos.Pharmacie::score).reversed().thenComparing(RisqueDtos.Pharmacie::pharmacieId))
                .toList();
    }
}
