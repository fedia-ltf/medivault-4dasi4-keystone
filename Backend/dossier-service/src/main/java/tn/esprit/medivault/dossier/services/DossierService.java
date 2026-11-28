package tn.esprit.medivault.dossier.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.dossier.dto.*;
import tn.esprit.medivault.dossier.entities.*;
import tn.esprit.medivault.dossier.exceptions.AccesRefuseException;
import tn.esprit.medivault.dossier.exceptions.DossierNotFoundException;
import tn.esprit.medivault.dossier.exceptions.RequeteInvalideException;
import tn.esprit.medivault.dossier.messaging.DossierEventPublisher;
import tn.esprit.medivault.dossier.messaging.events.ConsultationAjouteeEvent;
import tn.esprit.medivault.dossier.repositories.AccesDossierRepository;
import tn.esprit.medivault.dossier.repositories.ConsultationRepository;
import tn.esprit.medivault.dossier.repositories.DossierRepository;
import tn.esprit.medivault.dossier.repositories.LienDossierRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final ConsultationRepository consultationRepository;
    private final AccesDossierRepository accesDossierRepository;
    private final LienDossierRepository lienDossierRepository;
    private final DossierMapper dossierMapper;
    private final DossierEventPublisher eventPublisher;

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
        // Pas encore de dossier = aucune justification d'accès : réponse autorise=false (le service
        // appelant renvoie 403), et non 404, que le fallback Feign transformerait en 503.
        boolean autorise = dossierRepository.findByPatientId(patientId)
                .map(dossier -> accesDossierRepository.existsByDossierIdAndProfessionnelIdAndStatut(
                        dossier.getId(), professionnelId, StatutAcces.ACTIF))
                .orElse(false);

        return new AccesJustificationResponse(patientId, professionnelId, autorise);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationResponse> listerConsultations(
            Long dossierId, String medecinId, LocalDate du, LocalDate au, Pageable pageable) {

        // Vérification dossier
        dossierRepository.findById(dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé avec l'id : " + dossierId));

        // Validation des filtres de date
        if (du != null && au != null && du.isAfter(au)) {
            throw new RequeteInvalideException("La date de début 'du' (" + du + ") ne peut pas être postérieure à la date de fin 'au' (" + au + ").");
        }

        // Tri toujours par date décroissante
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "date")
        );

        Page<Consultation> page = consultationRepository.findFiltered(dossierId, medecinId, du, au, sorted);

        List<ConsultationResponse> content = page.getContent().stream()
                .map(dossierMapper::toConsultationResponse)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
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

        // Publication de l'événement APRÈS le save (même transaction) — solution simple sans @TransactionalEventListener.
        // Si RabbitMQ est indisponible, le publisher absorbe l'erreur et la consultation reste enregistrée.
        eventPublisher.publierConsultationAjoutee(new ConsultationAjouteeEvent(
                saved.getId(),
                dossier.getPatientId(),
                saved.getMedecinId(),
                saved.getAnalysesDemandees(),
                saved.getDate()
        ));

        return dossierMapper.toConsultationResponse(saved);
    }

    @Transactional
    public ConsultationResponse modifierConsultation(Long dossierId, Long consultationId, ConsultationUpdateRequest req) {
        // Le dossier doit exister (sinon 404)
        dossierRepository.findById(dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé avec l'id : " + dossierId));

        Consultation consultation = consultationRepository.findByIdAndDossierId(consultationId, dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucune consultation trouvée avec l'id " + consultationId + " pour le dossier " + dossierId));

        boolean acces = accesDossierRepository.existsByDossierIdAndProfessionnelIdAndStatut(
                dossierId, req.medecinId(), StatutAcces.ACTIF);

        if (!acces) {
            throw new AccesRefuseException(
                    "Le médecin " + req.medecinId() + " n'a pas d'accès actif au dossier " + dossierId);
        }

        if (!consultation.getMedecinId().equals(req.medecinId())) {
            throw new AccesRefuseException(
                    "Seul le médecin auteur (" + consultation.getMedecinId() + ") peut modifier cette consultation");
        }

        consultation.setMotif(req.motif());
        consultation.setDiagnostic(req.diagnostic());
        consultation.setObservations(req.observations());

        Consultation saved = consultationRepository.save(consultation);

        return dossierMapper.toConsultationResponse(saved);
    }

    @Transactional
    public void supprimerConsultation(Long dossierId, Long consultationId, String medecinId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé avec l'id : " + dossierId));

        Consultation consultation = consultationRepository.findByIdAndDossierId(consultationId, dossierId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucune consultation trouvée avec l'id " + consultationId + " pour le dossier " + dossierId));

        boolean acces = accesDossierRepository.existsByDossierIdAndProfessionnelIdAndStatut(
                dossierId, medecinId, StatutAcces.ACTIF);

        if (!acces) {
            throw new AccesRefuseException(
                    "Le médecin " + medecinId + " n'a pas d'accès actif au dossier " + dossierId);
        }

        if (!consultation.getMedecinId().equals(medecinId)) {
            throw new AccesRefuseException(
                    "Seul le médecin auteur (" + consultation.getMedecinId() + ") peut supprimer cette consultation");
        }

        dossier.getConsultations().removeIf(c -> c.getId() != null && c.getId().equals(consultationId));
        dossierRepository.save(dossier);
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
        Dossier dossier = creerDossierVide(patientId);

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

    @Transactional
    public void retirerAccesParPatient(String patientId, String professionnelId) {
        dossierRepository.findByPatientId(patientId).ifPresent(dossier -> {
            accesDossierRepository.findByDossierIdAndProfessionnelId(dossier.getId(), professionnelId)
                    .ifPresent(acces -> {
                        acces.setStatut(StatutAcces.RETIRE);
                        acces.setDateRetrait(LocalDateTime.now());
                        accesDossierRepository.save(acces);
                    });
        });
    }

    @Transactional
    public void ajouterLien(String patientId, TypeLien type, String referenceId, LocalDateTime date, String auteurId, String libelle) {
        Dossier dossier = creerDossierVide(patientId);

        boolean exists = lienDossierRepository.existsByDossierIdAndTypeAndReferenceId(dossier.getId(), type, referenceId);
        if (exists) {
            return;
        }

        LienDossier lien = LienDossier.builder()
                .type(type)
                .referenceId(referenceId)
                .date(date)
                .auteurId(auteurId)
                .libelle(libelle)
                .dossier(dossier)
                .build();

        lienDossierRepository.save(lien);
    }
}