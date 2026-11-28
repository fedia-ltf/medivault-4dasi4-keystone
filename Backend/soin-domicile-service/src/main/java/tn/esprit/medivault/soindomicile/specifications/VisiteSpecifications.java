package tn.esprit.medivault.soindomicile.specifications;

import org.springframework.data.jpa.domain.Specification;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import tn.esprit.medivault.soindomicile.entities.Visite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Critères de recherche des visites (Spring Data JPA Specifications).
 * Chaque critère est facultatif : seuls les critères remplis sont ajoutés à la requête (AND).
 */
public final class VisiteSpecifications {

    private VisiteSpecifications() { }

    public static Specification<Visite> recherche(String patientId, String pharmacieId, StatutVisite statut,
                                                  TypeSoin typeSoin, String texte, LocalDate du, LocalDate au) {
        List<Specification<Visite>> criteres = new ArrayList<>();
        if (estRempli(patientId)) criteres.add((v, q, cb) -> cb.equal(v.get("patientId"), patientId));
        if (estRempli(pharmacieId)) criteres.add((v, q, cb) -> cb.equal(v.get("pharmacieId"), pharmacieId));
        if (statut != null) criteres.add((v, q, cb) -> cb.equal(v.get("statut"), statut));
        if (typeSoin != null) criteres.add((v, q, cb) -> cb.equal(v.get("typeSoin"), typeSoin));
        if (du != null) criteres.add((v, q, cb) -> cb.greaterThanOrEqualTo(v.get("date"), du.atStartOfDay()));
        if (au != null) criteres.add((v, q, cb) -> cb.lessThan(v.get("date"), au.plusDays(1).atStartOfDay()));
        if (estRempli(texte)) {
            String motif = "%" + texte.trim().toLowerCase() + "%";
            // le texte peut être le patient, l'adresse ou l'infirmier affecté
            criteres.add((v, q, cb) -> cb.or(
                    cb.like(cb.lower(v.get("patientId")), motif),
                    cb.like(cb.lower(v.get("adresse")), motif),
                    cb.like(cb.lower(v.get("infirmierNom")), motif)));
        }
        return Specification.allOf(criteres);
    }

    private static boolean estRempli(String s) { return s != null && !s.isBlank(); }
}
