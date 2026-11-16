package tn.esprit.medivault.dossier.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.dossier.dto.*;
import tn.esprit.medivault.dossier.entities.*;
import tn.esprit.medivault.dossier.exceptions.AccesRefuseException;
import tn.esprit.medivault.dossier.exceptions.DossierNotFoundException;
import tn.esprit.medivault.dossier.repositories.AccesDossierRepository;
import tn.esprit.medivault.dossier.repositories.ConsultationRepository;
import tn.esprit.medivault.dossier.repositories.DossierRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final ConsultationRepository consultationRepository;
    private final AccesDossierRepository accesDossierRepository;
    private final DossierMapper dossierMapper;

    // ---- Lecture ----

    @Transactional(readOnly = true)
    public DossierResponse getDossierByPatientId(String patientId) {
        Dossier dossier = dossierRepository.findByPatientId(patientId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé pour le patient : " + patientId));

        List<ConsultationResponse> consultations = consultationRepository
                .findByDossierIdOrderByDateDesc(dossier.getId())
                .stream()
                .map(dossierMapper::toConsultationResponse)
                .toList();

        return dossierMapper.toDossierResponse(dossier, consultations);
    }

    @Transactional(readOnly = true)
    public AllergiesResponse getAllergies(String patientId) {
        Dossier dossier = dossierRepository.findByPatientId(patientId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé pour le patient : " + patientId));

        return dossierMapper.toAllergiesResponse(dossier);
    }

    @Transactional(readOnly = true)
    public AccesJustificationResponse verifierAcces(String patientId, String professionnelId) {
        Dossier dossier = dossierRepository.findByPatientId(patientId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé pour le patient : " + patientId));

        boolean autorise = accesDossierRepository.existsByDossierIdAndProfessionnelIdAndStatut(
                dossier.getId(), professionnelId, StatutAcces.ACTIF);

        return new AccesJustificationResponse(patientId, professionnelId, autorise);
    }

    // ---- Écriture ----

    @Transactional
    public ConsultationResponse ajouterConsultation(Long dossierId, ConsultationRequest req) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé avec l'id : " + dossierId));

        boolean acces = accesDossierRepository.existsByDossierIdAndProfessionnelIdAndStatut(
                dossierId, req.medecinId(), StatutAcces.ACTIF);

        if (!acces) {
            throw new AccesRefuseException(
                    "Le médecin " + req.medecinId() + " n'a pas d'accès actif au dossier " + dossierId);
        }

        Consultation consultation = Consultation.builder()
                .medecinId(req.medecinId())
                .date(LocalDateTime.now())
                .motif(req.motif())
                .diagnostic(req.diagnostic())
                .observations(req.observations())
                .analysesDemandees(req.analysesDemandees() != null ? new ArrayList<>(req.analysesDemandees()) : new ArrayList<>())
                .dossier(dossier)
                .build();

        Consultation saved = consultationRepository.save(consultation);
        return dossierMapper.toConsultationResponse(saved);
    }

    @Transactional
    public AccesResponse retirerAcces(Long dossierId, String professionnelId) {
        AccesDossier acces = accesDossierRepository
                .findByDossierIdAndProfessionnelId(dossierId, professionnelId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun accès trouvé pour le professionnel " + professionnelId
                                + " sur le dossier " + dossierId));

        acces.setStatut(StatutAcces.RETIRE);
        acces.setDateRetrait(LocalDateTime.now());

        AccesDossier saved = accesDossierRepository.save(acces);
        return dossierMapper.toAccesResponse(saved);
    }

    @Transactional
    public Dossier creerDossierVide(String patientId) {
        return dossierRepository.findByPatientId(patientId)
                .orElseGet(() -> dossierRepository.save(new Dossier(patientId)));
    }

    @Transactional
    public void accorderAcces(String patientId, String professionnelId) {
        Dossier dossier = dossierRepository.findByPatientId(patientId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé pour le patient : " + patientId));

        AccesDossier acces = accesDossierRepository
                .findByDossierIdAndProfessionnelId(dossier.getId(), professionnelId)
                .orElse(null);

        if (acces != null) {
            // Réactiver l'accès existant
            acces.setStatut(StatutAcces.ACTIF);
            acces.setDateAccord(LocalDateTime.now());
            acces.setDateRetrait(null);
            accesDossierRepository.save(acces);
        } else {
            // Créer un nouvel accès
            AccesDossier nouvelAcces = AccesDossier.builder()
                    .professionnelId(professionnelId)
                    .statut(StatutAcces.ACTIF)
                    .dateAccord(LocalDateTime.now())
                    .dossier(dossier)
                    .build();
            accesDossierRepository.save(nouvelAcces);
        }
    }
}
