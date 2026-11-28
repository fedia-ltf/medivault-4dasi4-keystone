package tn.esprit.medivault.soindomicile.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.*;
import tn.esprit.medivault.soindomicile.exceptions.*;
import tn.esprit.medivault.soindomicile.repositories.*;
import tn.esprit.medivault.soindomicile.specifications.PlanSuiviSpecifications;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanSuiviServiceImpl implements IPlanSuiviService {

    private final PlanSuiviRepository planRepo;
    private final VisiteRepository visiteRepo;

    @Override
    @Transactional
    public PlanSuivi creer(PlanSuiviRequest r) {
        if (r.dateFin().isBefore(r.dateDebut())) throw new RegleMetierException("dateFin avant dateDebut");
        PlanSuivi plan = planRepo.save(PlanSuivi.builder()
                .patientId(r.patientId()).medecinId(r.medecinId()).typeSoin(r.typeSoin())
                .frequence(r.frequence()).dateDebut(r.dateDebut()).dateFin(r.dateFin())
                .seuilAlerte(r.seuilAlerte())
                .pharmacieId(r.pharmacieId()).adresse(r.adresse()).heureVisite(r.heureVisite())
                .build());
        visiteRepo.saveAll(plan.genererVisites(LocalDateTime.now()));      // RG1
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanSuivi> lister(String patientId, String medecinId) {
        if (patientId != null) return planRepo.findByPatientId(patientId);
        if (medecinId != null) return planRepo.findByMedecinId(medecinId);
        return planRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanSuivi> rechercher(String medecinId, String patientId, TypeSoin typeSoin, String texte, Boolean enCours) {
        return planRepo.findAll(PlanSuiviSpecifications.recherche(medecinId, patientId, typeSoin, texte, enCours),
                Sort.by(Sort.Direction.DESC, "dateDebut"));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSuivi getById(Long id) {
        return planRepo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Plan " + id + " introuvable"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Visite> getVisites(Long id) {
        getById(id);
        return visiteRepo.findByPlan_IdOrderByDateAsc(id);
    }

    @Override
    @Transactional
    public PlanSuivi modifier(Long id, PlanSuiviUpdateRequest r) {
        PlanSuivi plan = getById(id);
        if (!plan.estEnCours()) throw new RegleMetierException("Plan terminé");
        annulerFutures(plan);
        plan.setFrequence(r.frequence());
        plan.setDateFin(r.dateFin());
        plan.setHeureVisite(r.heureVisite());
        plan.setSeuilAlerte(r.seuilAlerte());
        visiteRepo.saveAll(plan.genererVisites(LocalDateTime.now()));
        return planRepo.save(plan);
    }

    /** Arrêter un plan : sa date de fin devient aujourd'hui, ses visites futures sont annulées. */
    @Override
    @Transactional
    public void arreter(Long id) {
        PlanSuivi plan = getById(id);
        annulerFutures(plan);
        plan.setDateFin(LocalDate.now());
        planRepo.save(plan);
    }

    private void annulerFutures(PlanSuivi plan) {
        List<Visite> futures = visiteRepo.findByPlan_IdAndDateAfterAndStatutIn(
                plan.getId(), LocalDateTime.now(), VisiteServiceImpl.ACTIVES);
        futures.forEach(v -> v.setStatut(StatutVisite.ANNULEE));
        visiteRepo.saveAll(futures);
    }
}
