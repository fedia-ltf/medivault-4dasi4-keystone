package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.clients.OrdonnanceClient;
import tn.esprit.medivault.medicament.dto.*;
import tn.esprit.medivault.medicament.entities.Interaction;
import tn.esprit.medivault.medicament.entities.Medicament;
import tn.esprit.medivault.medicament.exceptions.MedicamentNotFoundException;
import tn.esprit.medivault.medicament.messaging.MedicamentEventPublisher;
import tn.esprit.medivault.medicament.repositories.InteractionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicamentServiceImpl implements IMedicamentService {

    private static final String MOTIF_PAR_DEFAUT = "Retrait du catalogue";

    private final MedicamentRepository medicamentRepository;
    private final InteractionRepository interactionRepository;
    private final OrdonnanceClient ordonnanceClient;
    private final MedicamentEventPublisher eventPublisher;

    @Value("${medivault.ordonnance.statut-actif:ACTIVE}")
    private String statutActif;

    @Override
    public List<Medicament> getCatalogue() {
        return medicamentRepository.findAll();
    }

    @Override
    public Medicament ajouterMedicament(MedicamentRequest request) {
        Medicament medicament = Medicament.builder()
                .nom_commercial(request.getNom_commercial().trim())
                .dci(request.getDci().trim())
                .classe_therapeutique(request.getClasse_therapeutique().trim())
                .forme(request.getForme())
                .dosage(request.getDosage())
                .build();
        return medicamentRepository.save(medicament);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResponse verifierCompatibilite(VerificationRequest request) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(request.getMedicament_ids()));
        List<String> allergies = request.getAllergies() == null ? List.of() : request.getAllergies();
        List<AlerteDTO> alertes = new ArrayList<>();

        Map<Long, Medicament> trouves = medicamentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Medicament::getId, Function.identity()));

        for (Long id : ids) {
            Medicament medicament = trouves.get(id);

            // RG6 : un médicament absent du catalogue (retiré) ne peut pas être prescrit
            if (medicament == null) {
                alertes.add(AlerteDTO.builder()
                        .type(TypeAlerte.MEDICAMENT_RETIRE)
                        .medicament_id(id)
                        .bloquante(true)
                        .message("Le médicament " + id + " n'existe pas ou a été retiré du catalogue")
                        .build());
                continue;
            }

            // RG4 : même classe thérapeutique qu'une allergie du patient
            for (String allergie : allergies) {
                if (medicament.estDeLaClasse(allergie)) {
                    alertes.add(AlerteDTO.builder()
                            .type(TypeAlerte.ALLERGIE)
                            .medicament_id(id)
                            .bloquante(true)
                            .message(medicament.getNom_commercial() + " appartient à la classe "
                                    + medicament.getClasse_therapeutique() + " (allergie du patient)")
                            .build());
                }
            }
        }

        // RG4 : interactions entre les médicaments de la liste (seule CONTRE_INDIQUEE est bloquante)
        if (trouves.size() > 1) {
            List<Interaction> interactions = interactionRepository.findInteractionsEntre(new ArrayList<>(trouves.keySet()));
            for (Interaction interaction : interactions) {
                alertes.add(AlerteDTO.builder()
                        .type(TypeAlerte.INTERACTION)
                        .medicament_id(interaction.getMedicament_a().getId())
                        .medicament_associe_id(interaction.getMedicament_b().getId())
                        .gravite(interaction.getGravite())
                        .bloquante(interaction.estBloquante())
                        .message(interaction.getMedicament_a().getNom_commercial() + " + "
                                + interaction.getMedicament_b().getNom_commercial() + " : "
                                + interaction.getDescription())
                        .build());
            }
        }

        boolean compatible = alertes.stream().noneMatch(AlerteDTO::isBloquante);
        return new VerificationResponse(compatible, alertes);
    }

    @Override
    @Transactional
    public void retirerMedicament(Long id, String motif) {
        Medicament medicament = medicamentRepository.findById(id)
                .orElseThrow(() -> new MedicamentNotFoundException(id));

        // Appel synchrone (Feign + circuit breaker) : en cas de panne, le fallback renvoie une liste vide
        List<OrdonnanceDTO> ordonnancesActives = ordonnanceClient.getOrdonnances(id, statutActif);
        if (!ordonnancesActives.isEmpty()) {
            log.warn("Retrait de {} : {} ordonnance(s) active(s) concernée(s) {}",
                    medicament.getNom_commercial(), ordonnancesActives.size(),
                    ordonnancesActives.stream().map(OrdonnanceDTO::getId).toList());
        }

        interactionRepository.deleteByMedicamentId(id);
        medicamentRepository.delete(medicament);

        String motifFinal = (motif == null || motif.isBlank()) ? MOTIF_PAR_DEFAUT : motif;
        eventPublisher.publierMedicamentRetire(new MedicamentRetireEvent(id, motifFinal, LocalDateTime.now()));
    }
}
