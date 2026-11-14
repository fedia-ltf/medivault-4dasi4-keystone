package tn.esprit.medivault.ordonnance.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.ordonnance.clients.DossierClient;
import tn.esprit.medivault.ordonnance.clients.MedicamentClient;

// FIX 1: Use the correct package names (dto, entities, repositories)
import tn.esprit.medivault.ordonnance.dto.*;
import tn.esprit.medivault.ordonnance.entity.*;
import tn.esprit.medivault.ordonnance.exceptions.*;
import tn.esprit.medivault.ordonnance.messaging.OrdonnanceCreeeEvent;
import tn.esprit.medivault.ordonnance.messaging.RabbitConfig;
import tn.esprit.medivault.ordonnance.repository.OrdonnanceRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class OrdonnanceService {
    private final OrdonnanceRepository repository;
    private final DossierClient dossierClient;
    private final MedicamentClient medicamentClient;
    private final RabbitTemplate rabbitTemplate;

    public OrdonnanceService(OrdonnanceRepository repository, DossierClient dossierClient, MedicamentClient medicamentClient, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.dossierClient = dossierClient;
        this.medicamentClient = medicamentClient;
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

        CompatibiliteResponseDTO compat;
        try {
            // FIX 2: Use the Lombok Setter for CompatibiliteRequestDTO to avoid constructor issues
            CompatibiliteRequestDTO compatReq = new CompatibiliteRequestDTO();
            compatReq.setPatientId(request.getPatientId());
            compatReq.setMedicamentIds(request.getLignes().stream().map(LigneOrdonnanceDTO::getMedicamentId).toList());
            compatReq.setAllergies(allergies);

            compat = medicamentClient.verifierCompatibilite(compatReq);
        } catch (ServiceIndisponibleException e) {
            return saveEnAttente(request);
        }

        if (!compat.isCompatible()) {
            throw new ConflitMedicamentException(compat.getConflits());
        }

        Ordonnance o = construire(request);
        o.setStatut(StatutOrdonnance.VALIDEE);
        Ordonnance saved = repository.save(o);

        OrdonnanceCreeeEvent event = new OrdonnanceCreeeEvent(
                saved.getId(), saved.getCode(), saved.getPatientId(), saved.getMedecinId(), saved.getDateEmission()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "ordonnance.creee", event);

        return toDto(saved);
    }

    public OrdonnanceResponseDTO findByCode(String code) {
        return repository.findByCode(code).map(this::toDto).orElseThrow(() -> new OrdonnanceNotFoundException(code));
    }

    public List<OrdonnanceResponseDTO> findByPatient(Long patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toDto).toList();
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

    @Transactional
    public void marquerDelivree(Long ordonnanceId) {
        repository.findById(ordonnanceId).ifPresent(o -> {
            o.setStatut(StatutOrdonnance.DELIVREE);
            repository.save(o);
        });
    }

    private OrdonnanceResponseDTO saveEnAttente(OrdonnanceRequestDTO request) {
        Ordonnance o = construire(request);
        o.setStatut(StatutOrdonnance.EN_ATTENTE_VERIFICATION);
        return toDto(repository.save(o));
    }

    private Ordonnance construire(OrdonnanceRequestDTO request) {
        Ordonnance o = new Ordonnance();
        String annee = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        o.setCode(String.format(Locale.ROOT, "ORD-%s-%04d", annee, repository.count() + 1));
        o.setPatientId(request.getPatientId());
        o.setMedecinId(request.getMedecinId());
        o.setConsultationId(request.getConsultationId());
        o.setDateEmission(LocalDate.now());
        o.setDateExpiration(LocalDate.now().plusMonths(3));

        ajouterLignes(o, request);
        return o;
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

    // FIX 3: Explicitly map LigneOrdonnance (Entity) to LigneOrdonnanceResponseDTO
    private OrdonnanceResponseDTO toDto(Ordonnance o) {
        OrdonnanceResponseDTO dto = new OrdonnanceResponseDTO();
        dto.setId(o.getId());
        dto.setCode(o.getCode());
        dto.setPatientId(o.getPatientId());
        dto.setMedecinId(o.getMedecinId());
        dto.setStatut(o.getStatut().name());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dto.setDateEmission(o.getDateEmission().format(formatter));
        dto.setDateExpiration(o.getDateExpiration().format(formatter));

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