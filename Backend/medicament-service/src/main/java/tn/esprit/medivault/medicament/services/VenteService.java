package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.medivault.medicament.dto.VenteDtos;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.entities.VenteSensible;
import tn.esprit.medivault.medicament.events.QuotaBientotEpuise;
import tn.esprit.medivault.medicament.events.SanctionPharmacie;
import tn.esprit.medivault.medicament.events.VenteSensibleEnregistree;
import tn.esprit.medivault.medicament.exceptions.RessourceIntrouvableException;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;
import tn.esprit.medivault.medicament.repositories.VenteSensibleRepository;
import tn.esprit.medivault.medicament.verification.OrdonnanceVerifiee;
import tn.esprit.medivault.medicament.verification.VerificationExterne;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// LE COEUR DU SERVICE : déclarer une vente (POST /ventes) en appliquant les règles de la documentation.
//
//   Contrôles dans l'ordre :
//     médicament inconnu ................................ 404
//     pharmacie inactive (user-service) ................. 403
//     user-service / ordonnance-service en panne ........ 503  (pas de vérification, pas de vente)
//     aucun quota ce mois-ci, ou quota SUSPENDUE ........ 403
//     RG1 : ordonnance VALIDEE avec ce médicament ....... sinon 409 + 1 avertissement
//     RG2 : quota du mois suffisant ..................... sinon 409 + 1 avertissement
//     RG3 : au 2e avertissement, le quota passe SUSPENDUE (événement SanctionPharmacie)
//   Tout va bien : vente enregistrée (201), quota mis à jour, événement VenteSensibleEnregistree.
@Service
@RequiredArgsConstructor
@Slf4j
public class VenteService {

    private final MedicamentSensibleRepository medicaments;
    private final AttributionRepository attributions;
    private final VenteSensibleRepository ventes;
    private final VerificationExterne verification;
    private final TentativeRefuseeService tentatives;
    private final ApplicationEventPublisher evenements;
    private final TransactionTemplate transaction;
    private final Clock clock;

    // Pas de @Transactional ici : les appels vers les autres services (lents, parfois en panne) se font
    // AVANT la transaction, pour ne pas garder une connexion à la base ouverte pendant l'attente.
    public ResultatVente declarer(VenteDtos.Requete requete) {
        String pharmacieId = requete.pharmacieId().trim();
        MedicamentSensible medicament = medicaments.findById(requete.medicamentId())
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament sensible introuvable : " + requete.medicamentId()));
        if (!verification.pharmacieActive(pharmacieId)) {
            String motif = "Cette pharmacie n'est pas active : aucune vente n'est autorisée.";
            tracer(medicament, pharmacieId, requete, MotifRefus.PHARMACIE_INACTIVE, motif);
            return ResultatVente.refus(HttpStatus.FORBIDDEN, "Pharmacie inactive", motif, null);
        }
        OrdonnanceVerifiee ordonnance = verification.ordonnance(requete.ordonnanceCode().trim());
        return transaction.execute(statut -> enregistrer(requete, medicament, pharmacieId, ordonnance));
    }

    // Tout ce qui suit est une seule transaction : vente, quota et avertissement sont enregistrés ensemble.
    private ResultatVente enregistrer(VenteDtos.Requete requete, MedicamentSensible medicament, String pharmacieId, OrdonnanceVerifiee ordonnance) {
        YearMonth mois = YearMonth.now(clock);
        // Le quota est verrouillé : deux ventes simultanées ne peuvent pas dépasser le quota (RG2)
        Attribution quota = attributions.trouverPourVente(pharmacieId, requete.medicamentId(), mois).orElse(null);
        if (quota == null) {
            String motif = "Aucun quota n'est attribué à cette pharmacie pour ce médicament ce mois-ci.";
            tracer(medicament, pharmacieId, requete, MotifRefus.AUCUN_QUOTA, motif);
            return ResultatVente.refus(HttpStatus.FORBIDDEN, "Aucun quota", motif, null);
        }
        if (quota.estSuspendue()) {
            String motif = "Cette pharmacie est suspendue pour " + quota.getMedicament().getNomCommercial() + " : vente impossible.";
            tracer(medicament, pharmacieId, requete, MotifRefus.PHARMACIE_SUSPENDUE, motif);
            return ResultatVente.refus(HttpStatus.FORBIDDEN, "Pharmacie suspendue", motif, quota);
        }

        // RG1 : justification par une ordonnance
        String motifOrdonnance = motifOrdonnanceRefusee(ordonnance, requete);
        if (motifOrdonnance != null) {
            return faute(quota, requete, MotifRefus.ORDONNANCE_NON_JUSTIFIEE, "Vente non justifiée", motifOrdonnance);
        }
        // RG2 : quota du mois
        if (requete.quantite() > quota.reste()) {
            return faute(quota, requete, MotifRefus.QUOTA_EPUISE, "Quota épuisé", "Quota du mois insuffisant : il reste " + Math.max(0, quota.reste())
                    + " boîte(s) pour " + requete.quantite() + " demandée(s).");
        }

        VenteSensible vente = ventes.save(VenteSensible.builder()
                .patientId(ordonnance.patientId())
                .ordonnanceId(ordonnance.id())
                .ordonnanceCode(ordonnance.code())
                .quantite(requete.quantite())
                .date(LocalDateTime.now(clock))
                .attribution(quota)
                .build());
        int pourcentageAvant = quota.pourcentageUtilise();
        quota.ajouterVente(requete.quantite());
        // Alerte à 80 % : UNE seule fois, au moment où la vente fait passer le quota sous puis au-dessus du seuil
        if (pourcentageAvant < Attribution.SEUIL_ALERTE && quota.pourcentageUtilise() >= Attribution.SEUIL_ALERTE) {
            evenements.publishEvent(new QuotaBientotEpuise(pharmacieId, medicament.getId(), medicament.getNomCommercial(),
                    quota.pourcentageUtilise(), quota.reste(), LocalDateTime.now(clock)));
        }
        // Publié APRES la validation de la transaction (voir EvenementsPublisher)
        evenements.publishEvent(new VenteSensibleEnregistree(ordonnance.id(), requete.medicamentId(),
                requete.quantite(), pharmacieId, vente.getDate()));
        return ResultatVente.accepte(VenteDtos.Reponse.de(vente));
    }

    // RG1 : renvoie la raison du refus, ou null si l'ordonnance justifie la vente
    private String motifOrdonnanceRefusee(OrdonnanceVerifiee ordonnance, VenteDtos.Requete requete) {
        if (ordonnance == null) {
            return "Ordonnance introuvable : " + requete.ordonnanceCode() + ".";
        }
        if (!ordonnance.valide()) {
            return "Cette ordonnance n'est pas validée (statut : " + ordonnance.statut() + ").";
        }
        var ligne = ordonnance.ligne(requete.medicamentId());
        if (ligne.isEmpty()) {
            return "Cette ordonnance ne contient pas ce médicament.";
        }
        Integer prescrite = ligne.get().quantite();
        if (prescrite != null) {
            long dejaVendu = ventes.quantiteDejaVendue(ordonnance.id(), requete.medicamentId());
            if (dejaVendu + requete.quantite() > prescrite) {
                return "Quantité supérieure à l'ordonnance : " + prescrite + " prescrite(s), " + dejaVendu + " déjà délivrée(s).";
            }
        }
        return null;
    }

    // RG3 : une faute = 1 avertissement ; au 2e, SUSPENDUE. Le pharmacien est prévenu (SanctionPharmacie).
    private ResultatVente faute(Attribution quota, VenteDtos.Requete requete, MotifRefus type, String titre, String motif) {
        quota.ajouterAvertissement();
        tracer(quota.getMedicament(), quota.getPharmacieId(), requete, type, motif);
        SanctionPharmacie.Niveau niveau = quota.estSuspendue() ? SanctionPharmacie.Niveau.SUSPENSION : SanctionPharmacie.Niveau.AVERTISSEMENT;
        evenements.publishEvent(new SanctionPharmacie(quota.getPharmacieId(), niveau,
                titre + " (" + quota.getMedicament().getNomCommercial() + ") : " + motif, LocalDateTime.now(clock)));
        log.warn("Faute de la pharmacie {} : {} -> {} avertissement(s), statut {}", quota.getPharmacieId(), motif,
                quota.getAvertissements(), quota.getStatut());
        return ResultatVente.refus(HttpStatus.CONFLICT, titre, motif, quota);
    }

    // Historique des refus : chaque refus laisse une trace (qui, quoi, quand, pourquoi)
    private void tracer(MedicamentSensible medicament, String pharmacieId, VenteDtos.Requete requete, MotifRefus type, String motif) {
        tentatives.tracer(pharmacieId, medicament.getId(), medicament.getNomCommercial(), requete.ordonnanceCode().trim(),
                requete.quantite(), type, motif);
    }

    // GET /ventes : qui a acheté, avec quelle ordonnance (filtres facultatifs)
    @Transactional(readOnly = true)
    public List<VenteDtos.Reponse> lister(String pharmacieId, Long medicamentId, YearMonth mois) {
        Specification<VenteSensible> critere = (racine, requete, cb) -> cb.conjunction();
        if (pharmacieId != null && !pharmacieId.isBlank()) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("attribution").get("pharmacieId"), pharmacieId.trim()));
        }
        if (medicamentId != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("attribution").get("medicament").get("id"), medicamentId));
        }
        if (mois != null) {
            critere = critere.and((r, q, cb) -> cb.equal(r.get("attribution").get("mois"), mois));
        }
        return ventes.findAll(critere, Sort.by(Sort.Direction.DESC, "date")).stream().map(VenteDtos.Reponse::de).toList();
    }

    @Transactional(readOnly = true)
    public VenteDtos.Reponse obtenir(Long id) {
        return ventes.findById(id).map(VenteDtos.Reponse::de)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente introuvable : " + id));
    }
}