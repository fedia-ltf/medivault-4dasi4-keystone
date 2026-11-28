package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.AttributionDtos.*;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.StatutAttribution;
import tn.esprit.medivault.medicament.exceptions.ConflitException;
import tn.esprit.medivault.medicament.exceptions.RessourceIntrouvableException;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;
import tn.esprit.medivault.medicament.repositories.VenteSensibleRepository;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

// CRUD des quotas (réservé à l'administrateur) + bilan, quota en lot, disponibilités et passage au nouveau mois (RG4).
@Service
@RequiredArgsConstructor
@Slf4j
public class AttributionService {

    private final AttributionRepository attributions;
    private final MedicamentSensibleRepository medicaments;
    private final VenteSensibleRepository ventes;
    private final Clock clock;

    public YearMonth moisCourant() {
        return YearMonth.now(clock);
    }

    // READ (liste) : tous les filtres sont facultatifs
    @Transactional(readOnly = true)
    public List<Reponse> lister(String pharmacieId, Long medicamentId, YearMonth mois, StatutAttribution statut) {
        Specification<Attribution> critere = (racine, requete, cb) -> cb.conjunction();
        if (pharmacieId != null && !pharmacieId.isBlank()) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("pharmacieId"), pharmacieId.trim()));
        }
        if (medicamentId != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("medicament").get("id"), medicamentId));
        }
        if (mois != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("mois"), mois));
        }
        if (statut != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("statut"), statut));
        }
        return attributions.findAll(critere, Sort.by(Sort.Direction.DESC, "mois").and(Sort.by("id"))).stream()
                .map(Reponse::de).toList();
    }

    @Transactional(readOnly = true)
    public Reponse obtenir(Long id) {
        return Reponse.de(trouver(id));
    }

    // CREATE : un seul quota par pharmacie, médicament et mois
    @Transactional
    public Reponse creer(Requete requete) {
        MedicamentSensible medicament = medicament(requete.medicamentId());
        String pharmacieId = requete.pharmacieId().trim();
        if (attributions.existsByPharmacieIdAndMedicamentIdAndMois(pharmacieId, medicament.getId(), requete.mois())) {
            throw new ConflitException("Cette pharmacie a déjà un quota pour ce médicament et ce mois.");
        }
        return Reponse.de(attributions.save(nouvelle(pharmacieId, medicament, requete.mois(), requete.quantiteAttribuee())));
    }

    // UPDATE : quantité et statut. Le quota ne peut pas passer sous ce qui est déjà vendu.
    // Remettre ACTIVE un quota SUSPENDUE lève la sanction : les avertissements repartent à 0.
    @Transactional
    public Reponse modifier(Long id, Modification modification) {
        Attribution a = trouver(id);
        if (modification.quantiteAttribuee() < a.getQuantiteVendue()) {
            throw new ConflitException("Le quota ne peut pas être inférieur aux " + a.getQuantiteVendue() + " boîtes déjà vendues.");
        }
        a.setQuantiteAttribuee(modification.quantiteAttribuee());
        if (modification.statut() != null) {
            if (modification.statut() == StatutAttribution.ACTIVE && a.estSuspendue()) {
                a.setAvertissements(0);
            }
            a.setStatut(modification.statut());
        }
        return Reponse.de(a);
    }

    // DELETE : interdit si des ventes existent (une vente n'est jamais supprimée)
    @Transactional
    public void supprimer(Long id) {
        Attribution a = trouver(id);
        if (ventes.existsByAttributionId(id)) {
            throw new ConflitException("Ce quota a déjà des ventes : il ne peut plus être supprimé.");
        }
        attributions.delete(a);
    }

    // Même quota pour plusieurs pharmacies : celles qui ont déjà un quota sont ignorées
    @Transactional
    public ResultatLot lot(Lot lot) {
        MedicamentSensible medicament = medicament(lot.medicamentId());
        List<Reponse> crees = new ArrayList<>();
        List<String> ignorees = new ArrayList<>();
        for (String id : new LinkedHashSet<>(lot.pharmacieIds().stream().map(String::trim).toList())) {
            if (attributions.existsByPharmacieIdAndMedicamentIdAndMois(id, medicament.getId(), lot.mois())) {
                ignorees.add(id);
            } else {
                crees.add(Reponse.de(attributions.save(nouvelle(id, medicament, lot.mois(), lot.quantiteAttribuee()))));
            }
        }
        return new ResultatLot(crees.size(), ignorees.size(), crees, ignorees);
    }

    // GET /attributions/{id}/bilan : reçu, vendu, reste, avertissements
    @Transactional(readOnly = true)
    public Bilan bilan(Long id) {
        return Bilan.de(trouver(id));
    }

    // GET /disponibilites : où trouver ce médicament ce mois-ci
    @Transactional(readOnly = true)
    public List<Disponibilite> disponibilites(Long medicamentId) {
        medicament(medicamentId);
        return attributions.findByMedicamentIdAndMois(medicamentId, moisCourant()).stream()
                .map(a -> new Disponibilite(a.getPharmacieId(), Math.max(0, a.reste()), etat(a)))
                .sorted(Comparator.comparing(Disponibilite::etat).thenComparing(Disponibilite::pharmacieId))
                .toList();
    }

    // INDISPONIBLE : suspendue ou plus rien. BIENTOT_EPUISE : il reste 10 % du quota ou moins.
    private Etat etat(Attribution a) {
        if (a.estSuspendue() || a.reste() <= 0) {
            return Etat.INDISPONIBLE;
        }
        return a.reste() * 10 <= a.getQuantiteAttribuee() ? Etat.BIENTOT_EPUISE : Etat.DISPONIBLE;
    }

    // RG4 : "le 1er du mois : bilan + nouveaux quotas créés automatiquement".
    // Pour chaque quota du mois précédent : on garde son bilan, puis on crée le quota du nouveau mois
    // (même pharmacie, même médicament, même quantité, ACTIVE, sans vente ni avertissement).
    @Transactional
    public Renouvellement renouveler(YearMonth nouveauMois) {
        YearMonth ancienMois = nouveauMois.minusMonths(1);
        List<Attribution> anciennes = attributions.findByMois(ancienMois);
        List<Bilan> bilans = anciennes.stream().map(Bilan::de).toList();
        int crees = 0;
        int dejaExistants = 0;
        for (Attribution ancienne : anciennes) {
            if (attributions.existsByPharmacieIdAndMedicamentIdAndMois(ancienne.getPharmacieId(), ancienne.getMedicament().getId(), nouveauMois)) {
                dejaExistants++;
            } else {
                attributions.save(nouvelle(ancienne.getPharmacieId(), ancienne.getMedicament(), nouveauMois, ancienne.getQuantiteAttribuee()));
                crees++;
            }
        }
        log.info("RG4 : bilan de {} ({} quotas), {} nouveaux quotas créés pour {}", ancienMois, bilans.size(), crees, nouveauMois);
        return new Renouvellement(ancienMois, nouveauMois, crees, dejaExistants, bilans);
    }

    private Attribution nouvelle(String pharmacieId, MedicamentSensible medicament, YearMonth mois, int quantite) {
        return Attribution.builder()
                .pharmacieId(pharmacieId)
                .medicament(medicament)
                .mois(mois)
                .quantiteAttribuee(quantite)
                .build();
    }

    private Attribution trouver(Long id) {
        return attributions.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Quota introuvable : " + id));
    }

    private MedicamentSensible medicament(Long id) {
        return medicaments.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament sensible introuvable : " + id));
    }
}