package tn.esprit.medivault.soindomicile.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.*;
import tn.esprit.medivault.soindomicile.exceptions.*;
import tn.esprit.medivault.soindomicile.clients.MedicamentClient;
import tn.esprit.medivault.soindomicile.messaging.SoinEventPublisher;
import tn.esprit.medivault.soindomicile.repositories.InfirmierRepository;
import tn.esprit.medivault.soindomicile.repositories.VisiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisiteServiceImpl implements IVisiteService {

    static final Set<StatutVisite> ACTIVES = Set.of(StatutVisite.DEMANDEE, StatutVisite.ACCEPTEE);

    private final VisiteRepository repo;
    private final VerificationOrdonnance verificationOrdonnance;   // étape 8
    private final MedicamentClient medicamentClient;               // étape 8
    private final SoinEventPublisher publisher;                    // étape 9
    private final InfirmierRepository infirmierRepo;

    @Override
    @Transactional
    public Visite demander(VisiteRequest r) {
        if (repo.existsByPatientIdAndDateAndStatutIn(r.patientId(), r.date(), ACTIVES)) {
            throw new RegleMetierException("Le patient a déjà une visite prévue à cette heure"); // RG3
        }
        verificationOrdonnance.verifier(r.typeSoin(), r.ordonnanceId(), r.patientId()); // RG2 (synchrone)
        Visite v = Visite.builder()
                .patientId(r.patientId()).pharmacieId(r.pharmacieId())
                .date(r.date()).adresse(r.adresse()).typeSoin(r.typeSoin())
                .latitude(r.latitude()).longitude(r.longitude())
                .ordonnanceId(r.ordonnanceId()).medicamentId(r.medicamentId())
                .medicamentSensible(r.medicamentSensible()).dose(r.dose())
                .statut(StatutVisite.DEMANDEE)
                .build();
        return repo.save(v);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Visite> lister(String patientId, String pharmacieId, StatutVisite statut) {
        if (pharmacieId != null) {
            return statut == null ? repo.findByPharmacieIdOrderByDateAsc(pharmacieId)
                    : repo.findByPharmacieIdAndStatutOrderByDateAsc(pharmacieId, statut);
        }
        if (patientId != null) return repo.findByPatientIdOrderByDateAsc(patientId);
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Visite getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Visite " + id + " introuvable"));
    }

    @Override
    @Transactional
    public Visite modifier(Long id, VisiteUpdateRequest r) {
        Visite v = getById(id);
        exiger(v, StatutVisite.DEMANDEE);
        v.setDate(r.date());
        v.setAdresse(r.adresse());
        return repo.save(v);
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        Visite v = getById(id);
        exiger(v, StatutVisite.DEMANDEE); // une visite acceptée s'annule, elle ne se supprime pas
        repo.delete(v);
    }

    @Override
    @Transactional
    public Visite accepter(Long id, AcceptationRequest r) {
        Visite v = getById(id);
        exiger(v, StatutVisite.DEMANDEE);
        Infirmier inf = infirmierRepo.findById(r.infirmierId())
                .orElseThrow(() -> new RessourceIntrouvableException("Infirmier " + r.infirmierId() + " introuvable"));
        if (!inf.getPharmacieId().equals(v.getPharmacieId())) {
            throw new RegleMetierException(inf.getNomComplet() + " ne travaille pas dans cette pharmacie");
        }
        // RG9 : on n'affecte qu'un infirmier disponible
        //   1) la visite tombe dans ses horaires de disponibilité
        if (!inf.disponibleA(v.getDate().toLocalTime())) {
            throw new RegleMetierException(inf.getNomComplet() + " n'est disponible que de "
                    + inf.getHeureDebut() + " à " + inf.getHeureFin());
        }
        //   2) il n'a pas d'autre soin confirmé à moins d'une heure
        if (repo.existsByPharmacieIdAndInfirmierNomAndStatutAndDateBetween(v.getPharmacieId(), inf.getNomComplet(),
                StatutVisite.ACCEPTEE, v.getDate().minusMinutes(59), v.getDate().plusMinutes(59))) {
            throw new RegleMetierException(inf.getNomComplet() + " a déjà un soin à cette heure");
        }
        if (v.isMedicamentSensible()) {                                    // synchrone
            medicamentClient.enregistrerVente(
                    new VenteRequest(v.getPharmacieId(), v.getMedicamentId(), v.getOrdonnanceId(), 1));
        }
        v.setPharmacienId(r.pharmacienId());
        v.setInfirmierNom(inf.getNomComplet());                // copié : l'historique reste juste même si l'infirmier est supprimé
        v.setInfirmierTelephone(inf.getTelephone());
        v.setStatut(StatutVisite.ACCEPTEE);
        return repo.save(v);
    }

    @Override
    @Transactional
    public Visite saisirRapport(Long id, RapportRequest r) {
        Visite v = getById(id);
        exiger(v, StatutVisite.ACCEPTEE);                                  // RG7
        if (!r.pharmacienId().equals(v.getPharmacienId())) {
            throw new AccesRefuseException("Seul le pharmacien qui a accepté la visite saisit le rapport");
        }
        v.setTension(r.tension());
        v.setGlycemie(r.glycemie());
        v.setInjectionFaite(Boolean.TRUE.equals(r.injectionFaite()));
        if (r.dose() != null) v.setDose(r.dose());
        v.setObservations(r.observations());
        v.setDateRapport(LocalDateTime.now());
        v.setStatut(StatutVisite.REALISEE);
        Visite saved = repo.save(v);
        publisher.publierSoinRealise(new SoinRealiseEvent(saved.getId(), saved.getPatientId(),
                saved.getPharmacieId(), saved.getPharmacienId(), saved.getInfirmierNom(), saved.getTypeSoin().name(), saved.getDate(),
                saved.getTension(), saved.getGlycemie(), saved.isInjectionFaite(),
                saved.getDose(), saved.getObservations()));                              // RG6

        PlanSuivi plan = saved.getPlan();
        if (plan != null && saved.estAnormale(plan.getSeuilAlerte())) {                  // RG4
            publisher.publierAlerteMesure(new AlerteMesureEvent(saved.getId(), saved.getPatientId(),
                    plan.getMedecinId(), saved.getTension(), saved.getGlycemie(),
                    plan.getSeuilAlerte(), saved.getDateRapport()));
        }
        return saved;
    }

    @Override
    @Transactional
    public Visite annuler(Long id) {
        Visite v = getById(id);
        if (!v.peutEtreAnnulee(LocalDateTime.now())) {                    // RG5
            throw new RegleMetierException("Annulation impossible moins de 2 h avant la visite");
        }
        v.setStatut(StatutVisite.ANNULEE);
        return repo.save(v);
    }

    @Override
    @Transactional
    public int annulerInjections(Long medicamentId, String motif) {      // RG8
        List<Visite> liste = repo.findByMedicamentIdAndTypeSoinAndStatutIn(medicamentId, TypeSoin.INJECTION, ACTIVES);
        liste.forEach(v -> {
            v.setStatut(StatutVisite.ANNULEE);
            v.setObservations("Annulée : médicament retiré du marché (" + motif + ")");
        });
        repo.saveAll(liste);
        return liste.size();
    }

    private static void exiger(Visite v, StatutVisite attendu) {
        if (v.getStatut() != attendu) {
            throw new RegleMetierException("La visite " + v.getId() + " est " + v.getStatut());
        }
    }
}
