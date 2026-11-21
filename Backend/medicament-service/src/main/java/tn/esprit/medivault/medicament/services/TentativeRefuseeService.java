package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.TentativeDtos;
import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;
import tn.esprit.medivault.medicament.repositories.TentativeRefuseeRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// Historique des ventes refusées : enregistré par VenteService à chaque refus, consulté par l'administrateur.
@Service
@RequiredArgsConstructor
public class TentativeRefuseeService {

    private final TentativeRefuseeRepository tentatives;
    private final Clock clock;

    // Dans la transaction de la vente si elle existe (le refus et l'avertissement sont enregistrés ensemble),
    // sinon dans la sienne (pharmacie inactive : aucune transaction de vente n'est ouverte).
    @Transactional
    public void tracer(String pharmacieId, Long medicamentId, String medicamentNom, String ordonnanceCode,
                       int quantite, MotifRefus motif, String detail) {
        tentatives.save(TentativeRefusee.builder()
                .pharmacieId(pharmacieId).medicamentId(medicamentId).medicamentNom(medicamentNom)
                .ordonnanceCode(ordonnanceCode).quantite(quantite).date(LocalDateTime.now(clock))
                .motif(motif).detail(detail == null ? null : detail.substring(0, Math.min(detail.length(), 500))).build());
    }

    // GET /tentatives-refusees : les plus récentes d'abord, filtres facultatifs
    @Transactional(readOnly = true)
    public List<TentativeDtos.Reponse> lister(String pharmacieId, Long medicamentId, YearMonth mois, MotifRefus motif) {
        Specification<TentativeRefusee> critere = (racine, requete, cb) -> cb.conjunction();
        if (pharmacieId != null && !pharmacieId.isBlank()) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("pharmacieId"), pharmacieId.trim()));
        }
        if (medicamentId != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("medicamentId"), medicamentId));
        }
        if (motif != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("motif"), motif));
        }
        if (mois != null) {
            LocalDateTime debut = mois.atDay(1).atStartOfDay(), fin = mois.plusMonths(1).atDay(1).atStartOfDay();
            critere = critere.and((r, q, cb) -> cb.and(cb.greaterThanOrEqualTo(r.get("date"), debut), cb.lessThan(r.get("date"), fin)));
        }
        return tentatives.findAll(critere, Sort.by(Sort.Direction.DESC, "date", "id")).stream().map(TentativeDtos.Reponse::de).toList();
    }
}
