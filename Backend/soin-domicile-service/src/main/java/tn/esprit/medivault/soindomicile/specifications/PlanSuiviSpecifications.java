package tn.esprit.medivault.soindomicile.specifications;

import org.springframework.data.jpa.domain.Specification;
import tn.esprit.medivault.soindomicile.entities.PlanSuivi;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Critères de recherche des plans de suivi : seuls les critères remplis sont appliqués (AND). */
public final class PlanSuiviSpecifications {

    private PlanSuiviSpecifications() { }

    public static Specification<PlanSuivi> recherche(String medecinId, String patientId, TypeSoin typeSoin,
                                                     String texte, Boolean enCours) {
        List<Specification<PlanSuivi>> criteres = new ArrayList<>();
        if (estRempli(medecinId)) criteres.add((p, q, cb) -> cb.equal(p.get("medecinId"), medecinId));
        if (estRempli(patientId)) criteres.add((p, q, cb) -> cb.equal(p.get("patientId"), patientId));
        if (typeSoin != null) criteres.add((p, q, cb) -> cb.equal(p.get("typeSoin"), typeSoin));
        if (enCours != null) {
            LocalDate aujourdhui = LocalDate.now();
            criteres.add(enCours
                    ? (p, q, cb) -> cb.greaterThanOrEqualTo(p.get("dateFin"), aujourdhui)
                    : (p, q, cb) -> cb.lessThan(p.get("dateFin"), aujourdhui));
        }
        if (estRempli(texte)) {
            String motif = "%" + texte.trim().toLowerCase() + "%";
            criteres.add((p, q, cb) -> cb.or(
                    cb.like(cb.lower(p.get("patientId")), motif),
                    cb.like(cb.lower(p.get("adresse")), motif)));
        }
        return Specification.allOf(criteres);
    }

    private static boolean estRempli(String s) { return s != null && !s.isBlank(); }
}
