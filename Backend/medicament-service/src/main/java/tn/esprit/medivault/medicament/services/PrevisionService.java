package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.PrevisionDtos;
import tn.esprit.medivault.medicament.dto.PrevisionDtos.Niveau;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// PRÉVISION D'ÉPUISEMENT : « à ce rythme, le quota de Subutex sera épuisé vers le 23 ».
// Méthode volontairement simple et expliquable : rythme = boîtes vendues / jours écoulés du mois,
// puis jours restants = reste / rythme. Aucune donnée venant d'un autre service.
@Service
@RequiredArgsConstructor
public class PrevisionService {

    private final AttributionRepository attributions;
    private final Clock clock;

    // Résultat du calcul (utilisé tel quel par les tests unitaires)
    public record Calcul(int reste, int pourcentage, double rythme, LocalDate dateEpuisement,
                         boolean avantFinDeMois, Niveau niveau, String message) {
    }

    // Le cœur du calcul : une fonction pure (mêmes entrées = mêmes sorties), donc facile à tester.
    public static Calcul calculer(int attribue, int vendu, YearMonth mois, LocalDate aujourdhui) {
        int reste = attribue - vendu;
        int pourcentage = attribue > 0 ? (int) (vendu * 100L / attribue) : 0;   // même règle que l'alerte à 80 % (Attribution)
        boolean enCours = mois.equals(YearMonth.from(aujourdhui));
        boolean alerte = pourcentage >= Attribution.SEUIL_ALERTE;

        if (attribue > 0 && reste <= 0) {
            return new Calcul(Math.max(0, reste), pourcentage, 0, null, false, Niveau.EPUISE, "Quota épuisé : plus aucune vente possible ce mois-ci.");
        }
        if (!enCours) {
            String message = mois.isBefore(YearMonth.from(aujourdhui))
                    ? "Mois terminé : " + pourcentage + " % du quota utilisé."
                    : "Mois à venir : aucune vente pour l'instant.";
            return new Calcul(reste, pourcentage, 0, null, false, alerte ? Niveau.ALERTE : Niveau.OK, message);
        }
        double rythme = vendu / (double) aujourdhui.getDayOfMonth();
        if (vendu == 0 || attribue <= 0) {
            return new Calcul(reste, pourcentage, 0, null, false, Niveau.OK, "Aucune vente ce mois-ci : pas d'épuisement prévu.");
        }
        int joursRestants = (int) Math.ceil(reste / rythme);
        LocalDate date = aujourdhui.plusDays(joursRestants);
        boolean avant = !date.isAfter(mois.atEndOfMonth());
        String phrase = avant
                ? "À ce rythme, le quota sera épuisé vers le " + date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)) + "."
                : "À ce rythme, le quota suffira jusqu'à la fin du mois.";
        if (alerte) {
            phrase = "Attention : " + pourcentage + " % du quota est déjà utilisé. " + phrase;
        }
        Niveau niveau = alerte ? Niveau.ALERTE : avant ? Niveau.A_SURVEILLER : Niveau.OK;
        return new Calcul(reste, pourcentage, rythme, avant ? date : null, avant, niveau, phrase);
    }

    private static double arrondi(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // GET /previsions : une prévision par quota (filtres facultatifs ; le mois courant par défaut)
    @Transactional(readOnly = true)
    public List<PrevisionDtos.Quota> parQuota(String pharmacieId, Long medicamentId, YearMonth mois) {
        YearMonth m = mois != null ? mois : YearMonth.now(clock);
        LocalDate aujourdhui = LocalDate.now(clock);
        Specification<Attribution> critere = (r, q, cb) -> cb.equal(r.get("mois"), m);
        if (pharmacieId != null && !pharmacieId.isBlank()) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("pharmacieId"), pharmacieId.trim()));
        }
        if (medicamentId != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("medicament").get("id"), medicamentId));
        }
        return attributions.findAll(critere, Sort.by("pharmacieId", "id")).stream().map(a -> {
            Calcul c = calculer(a.getQuantiteAttribuee(), a.getQuantiteVendue(), m, aujourdhui);
            return new PrevisionDtos.Quota(a.getId(), a.getPharmacieId(), a.getMedicament().getId(), a.getMedicament().getNomCommercial(), m,
                    a.getQuantiteAttribuee(), a.getQuantiteVendue(), c.reste(), c.pourcentage(), arrondi(c.rythme()),
                    c.dateEpuisement(), c.avantFinDeMois(), c.niveau(), c.message());
        }).toList();
    }

    // GET /previsions/medicaments : une prévision par médicament, toutes pharmacies confondues
    @Transactional(readOnly = true)
    public List<PrevisionDtos.Medicament> parMedicament(YearMonth mois) {
        YearMonth m = mois != null ? mois : YearMonth.now(clock);
        LocalDate aujourdhui = LocalDate.now(clock);
        Map<Long, List<Attribution>> groupes = new LinkedHashMap<>();
        for (Attribution a : attributions.findAll((r, q, cb) -> cb.equal(r.get("mois"), m), Sort.by("medicament.nomCommercial"))) {
            groupes.computeIfAbsent(a.getMedicament().getId(), k -> new java.util.ArrayList<>()).add(a);
        }
        return groupes.values().stream().map(liste -> {
            int attribue = liste.stream().mapToInt(Attribution::getQuantiteAttribuee).sum();
            int vendu = liste.stream().mapToInt(Attribution::getQuantiteVendue).sum();
            Calcul c = calculer(attribue, vendu, m, aujourdhui);
            int enAlerte = (int) liste.stream().filter(a -> {
                Niveau n = calculer(a.getQuantiteAttribuee(), a.getQuantiteVendue(), m, aujourdhui).niveau();
                return n == Niveau.ALERTE || n == Niveau.EPUISE;
            }).count();
            var premier = liste.get(0).getMedicament();
            return new PrevisionDtos.Medicament(premier.getId(), premier.getNomCommercial(), m, liste.size(), attribue, vendu,
                    c.reste(), c.pourcentage(), arrondi(c.rythme()), c.dateEpuisement(), c.avantFinDeMois(), c.niveau(), enAlerte, c.message());
        }).toList();
    }
}
