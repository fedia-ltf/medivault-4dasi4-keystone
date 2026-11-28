package tn.esprit.medivault.ordonnance.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import tn.esprit.medivault.ordonnance.clients.DossierClient;

import tn.esprit.medivault.ordonnance.dto.*;
import tn.esprit.medivault.ordonnance.entity.*;
import tn.esprit.medivault.ordonnance.exceptions.*;
import tn.esprit.medivault.ordonnance.messaging.OrdonnanceCreeeEvent;
import tn.esprit.medivault.ordonnance.messaging.RabbitConfig;
import tn.esprit.medivault.ordonnance.messaging.VenteSensibleEnregistreeEvent;
import tn.esprit.medivault.ordonnance.repository.OrdonnanceRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrdonnanceService {
    private final OrdonnanceRepository repository;
    private final DossierClient dossierClient;
    private final RabbitTemplate rabbitTemplate;

    public OrdonnanceService(OrdonnanceRepository repository, DossierClient dossierClient, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.dossierClient = dossierClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public OrdonnanceResponseDTO creer(OrdonnanceRequestDTO request) {
        List<String> allergies;
        try {
            allergies = dossierClient.getAllergies(request.getPatientId()).getAllergies();
        } catch (ServiceIndisponibleException e) {
            return saveEnAttente(request);
        }

        // Contrôle des allergies du dossier (dossier-service) : un médicament dont le nom contient
        // une allergie déclarée est refusé (409). medicament-service ne gère que les médicaments
        // sensibles (quotas) : il n'expose pas de contrôle de compatibilité.
        List<String> conflits = conflitsAllergies(request, allergies);
        if (!conflits.isEmpty()) {
            throw new ConflitMedicamentException(conflits);
        }

        Ordonnance o = construire(request);
        o.setStatut(StatutOrdonnance.VALIDEE);
        Ordonnance saved = repository.save(o);

        OrdonnanceCreeeEvent event = new OrdonnanceCreeeEvent(
                saved.getId(), saved.getCode(), saved.getPatientId(), saved.getMedecinId(), saved.getDateEmission()
        );
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "ordonnance.creee", event);
        } catch (AmqpException e) {
            log.warn("Ordonnance {} enregistrée, mais événement non envoyé : RabbitMQ indisponible",
                    saved.getCode(), e);
        }

        return toDto(saved);
    }

    public OrdonnanceResponseDTO findById(Long ordonnanceId) {
        return repository.findById(ordonnanceId).map(this::toDto)
                .orElseThrow(() -> new OrdonnanceNotFoundException(String.valueOf(ordonnanceId)));
    }

    public OrdonnanceResponseDTO findByCode(String code) {
        return repository.findByCode(code).map(this::toDto).orElseThrow(() -> new OrdonnanceNotFoundException(code));
    }

    public List<OrdonnanceResponseDTO> findByPatient(String patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toDto).toList();
    }

    public List<OrdonnanceResponseDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public List<String> findPatientIds(String query) {
        String recherche = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .map(Ordonnance::getPatientId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .filter(patientId -> recherche.isEmpty()
                        || patientId.toLowerCase(Locale.ROOT).contains(recherche)
                        || ("patient #" + patientId).contains(recherche)
                        || ("patient " + patientId).contains(recherche))
                .sorted()
                .collect(Collectors.toList());
    }

    public List<OrdonnanceResponseDTO> findByMedicamentEtStatut(Long medicamentId, StatutOrdonnance statut) {
        return repository.findByLignesMedicamentIdAndStatut(medicamentId, statut).stream().map(this::toDto).toList();
    }

    @Transactional
    public OrdonnanceResponseDTO modifier(Long ordonnanceId, OrdonnanceRequestDTO request) {
        Ordonnance ordonnance = repository.findById(ordonnanceId)
                .orElseThrow(() -> new OrdonnanceNotFoundException(String.valueOf(ordonnanceId)));

        ordonnance.setPatientId(request.getPatientId());
        ordonnance.setMedecinId(request.getMedecinId());
        ordonnance.setConsultationId(request.getConsultationId());
        ordonnance.getLignes().clear();
        ajouterLignes(ordonnance, request);

        return toDto(repository.save(ordonnance));
    }

    @Transactional
    public void supprimer(Long ordonnanceId) {
        if (!repository.existsById(ordonnanceId)) {
            throw new OrdonnanceNotFoundException(String.valueOf(ordonnanceId));
        }
        repository.deleteById(ordonnanceId);
    }

    /**
     * Événement vente.sensible.enregistree (medicament-service) : la pharmacie a délivré une quantité
     * d'un médicament de l'ordonnance. On augmente quantiteDelivree de la ligne, puis le statut passe
     * à PARTIELLEMENT_DELIVREE ou DELIVREE quand toutes les lignes sont servies.
     */
    @Transactional
    public void appliquerVente(VenteSensibleEnregistreeEvent vente) {
        if (vente.getOrdonnanceId() == null) {
            return;
        }
        repository.findById(vente.getOrdonnanceId()).ifPresentOrElse(o -> {
            o.getLignes().stream()
                    .filter(l -> l.getMedicamentId() != null && l.getMedicamentId().equals(vente.getMedicamentId()))
                    .findFirst()
                    .ifPresentOrElse(l -> {
                        l.setQuantiteDelivree(Math.min(l.getQuantite(), l.getQuantiteDelivree() + vente.getQuantite()));
                    }, () -> log.warn("Vente reçue pour le médicament {} absent de l'ordonnance {}",
                            vente.getMedicamentId(), o.getCode()));
            boolean toutServi = o.getLignes().stream()
                    .allMatch(l -> l.getQuantiteDelivree() >= l.getQuantite());
            o.setStatut(toutServi ? StatutOrdonnance.DELIVREE : StatutOrdonnance.PARTIELLEMENT_DELIVREE);
            repository.save(o);
            log.info("Vente appliquée à l'ordonnance {} : statut {}", o.getCode(), o.getStatut());
        }, () -> log.warn("Vente reçue pour une ordonnance inconnue : {}", vente.getOrdonnanceId()));
    }

    @Transactional
    public void marquerDelivree(Long ordonnanceId) {
        repository.findById(ordonnanceId).ifPresent(o -> {
            o.setStatut(StatutOrdonnance.DELIVREE);
            repository.save(o);
        });
    }

    @Transactional
    public OrdonnanceResponseDTO modifierStatut(Long ordonnanceId, StatutOrdonnance statut) {
        Ordonnance ordonnance = repository.findById(ordonnanceId)
                .orElseThrow(() -> new OrdonnanceNotFoundException(String.valueOf(ordonnanceId)));
        if (ordonnance.isStatutModifieParPharmacien()) {
            throw new StatutOrdonnanceVerrouilleeException(ordonnance.getCode());
        }
        ordonnance.setStatut(statut);
        ordonnance.setStatutModifieParPharmacien(true);
        return toDto(repository.save(ordonnance));
    }

    private OrdonnanceResponseDTO saveEnAttente(OrdonnanceRequestDTO request) {
        Ordonnance o = construire(request);
        o.setStatut(StatutOrdonnance.EN_ATTENTE_VERIFICATION);
        return toDto(repository.save(o));
    }

    private Ordonnance construire(OrdonnanceRequestDTO request) {
        Ordonnance o = new Ordonnance();
        o.setCode(prochainCode());
        o.setPatientId(request.getPatientId());
        o.setMedecinId(request.getMedecinId());
        o.setConsultationId(request.getConsultationId());
        o.setDateEmission(LocalDate.now());
        o.setDateExpiration(LocalDate.now().plusMonths(3));

        ajouterLignes(o, request);
        return o;
    }

    private List<String> conflitsAllergies(OrdonnanceRequestDTO request, List<String> allergies) {
        if (allergies == null || allergies.isEmpty() || request.getLignes() == null) {
            return List.of();
        }
        return request.getLignes().stream()
                .filter(l -> l.getNomMedicament() != null)
                .filter(l -> allergies.stream()
                        .filter(a -> a != null && !a.isBlank())
                        .anyMatch(a -> l.getNomMedicament().toLowerCase(Locale.ROOT)
                                .contains(a.trim().toLowerCase(Locale.ROOT))))
                .map(l -> l.getNomMedicament() + " : allergie déclarée dans le dossier du patient")
                .toList();
    }

    private String prochainCode() {
        String annee = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long numero = repository.count() + 1;
        String code = String.format(Locale.ROOT, "ORD-%s-%04d", annee, numero);
        while (repository.existsByCode(code)) {
            numero++;
            code = String.format(Locale.ROOT, "ORD-%s-%04d", annee, numero);
        }
        return code;
    }

    private void ajouterLignes(Ordonnance o, OrdonnanceRequestDTO request) {
        for (LigneOrdonnanceDTO l : request.getLignes()) {
            LigneOrdonnance ligne = new LigneOrdonnance();
            ligne.setMedicamentId(l.getMedicamentId());
            ligne.setNomMedicament(l.getNomMedicament());
            ligne.setPosologie(l.getPosologie());
            ligne.setDureeJours(l.getDureeJours());
            ligne.setQuantite(l.getQuantite());
            ligne.setQuantiteDelivree(0);
            ligne.setOrdonnance(o);
            o.getLignes().add(ligne);
        }
    }

    private OrdonnanceResponseDTO toDto(Ordonnance o) {
        OrdonnanceResponseDTO dto = new OrdonnanceResponseDTO();
        dto.setId(o.getId());
        dto.setCode(o.getCode());
        dto.setPatientId(o.getPatientId());
        dto.setMedecinId(o.getMedecinId());
        dto.setStatut(o.getStatut().name());

        // Format ISO (2026-11-28) : lu comme LocalDate par soin-domicile-service
        dto.setDateEmission(o.getDateEmission().toString());
        dto.setDateExpiration(o.getDateExpiration().toString());
        dto.setStatutModifieParPharmacien(o.isStatutModifieParPharmacien());

        List<LigneOrdonnanceResponseDTO> lignesDto = o.getLignes().stream().map(l -> {
            LigneOrdonnanceResponseDTO lDto = new LigneOrdonnanceResponseDTO();
            lDto.setId(l.getId());
            lDto.setMedicamentId(l.getMedicamentId());
            lDto.setNomMedicament(l.getNomMedicament());
            lDto.setPosologie(l.getPosologie());
            lDto.setDureeJours(l.getDureeJours());
            lDto.setQuantite(l.getQuantite());
            lDto.setQuantiteDelivree(l.getQuantiteDelivree());
            return lDto;
        }).toList();

        dto.setLignes(lignesDto);
        return dto;
    }
}