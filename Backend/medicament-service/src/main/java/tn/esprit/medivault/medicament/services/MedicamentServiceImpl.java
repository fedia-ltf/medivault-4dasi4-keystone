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

// LE FICHIER LE PLUS IMPORTANT : toute la logique métier (RG4, RG6) est ici.
// Le controller reçoit la requête et appelle ces méthodes ; il ne contient aucune règle.
// @Service : objet géré par Spring. @RequiredArgsConstructor : injection des 4 champs final.
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicamentServiceImpl implements IMedicamentService {

    private static final String MOTIF_PAR_DEFAUT = "Retrait du catalogue";

    // Les "outils" dont le service a besoin, injectés par Spring :
    private final MedicamentRepository medicamentRepository;   // table medicament
    private final InteractionRepository interactionRepository; // table interaction
    private final OrdonnanceClient ordonnanceClient;           // appel Feign vers ordonnance-service
    private final MedicamentEventPublisher eventPublisher;     // envoi d'événements RabbitMQ

    // @Value : lit la valeur dans application.properties (medivault.ordonnance.statut-actif).
    // ":ACTIVE" = valeur par défaut si la clé n'existe pas.
    @Value("${medivault.ordonnance.statut-actif:ACTIVE}")
    private String statutActif;

    // READ : tout le catalogue (SELECT * FROM medicament)
    @Override
    public List<Medicament> getCatalogue() {
        return medicamentRepository.findAll();
    }

    // CREATE : on transforme le DTO (MedicamentRequest) en entité (Medicament), puis on l'enregistre.
    // .trim() enlève les espaces au début et à la fin.
    // save() fait l'INSERT et renvoie le médicament avec son id généré.
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

    // MÉTIER AVANCÉ : vérifier la compatibilité d'une prescription.
    // Étapes : 1) RG6 médicament retiré ?  2) RG4 allergie ?  3) RG4 interactions ?
    // readOnly = true : transaction en lecture seule (on ne modifie rien).
    @Override
    @Transactional(readOnly = true)
    public VerificationResponse verifierCompatibilite(VerificationRequest request) {
        // LinkedHashSet enlève les doublons en gardant l'ordre : [3, 7, 3] -> [3, 7]
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(request.getMedicament_ids()));
        // Si aucune allergie n'est envoyée, on utilise une liste vide (évite un NullPointerException)
        List<String> allergies = request.getAllergies() == null ? List.of() : request.getAllergies();
        List<AlerteDTO> alertes = new ArrayList<>();

        // On charge tous les médicaments en UNE seule requête, puis on les range dans une Map
        // id -> Medicament, pour les retrouver vite : trouves.get(3) -> le médicament 3
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
                continue; // on passe au médicament suivant
            }

            // RG4 : même classe thérapeutique qu'une allergie du patient -> alerte bloquante
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

        // RG4 : interactions entre les médicaments de la liste (seule CONTRE_INDIQUEE est bloquante).
        // Il faut au moins 2 médicaments pour avoir une interaction.
        if (trouves.size() > 1) {
            List<Interaction> interactions = interactionRepository.findInteractionsEntre(new ArrayList<>(trouves.keySet()));
            for (Interaction interaction : interactions) {
                alertes.add(AlerteDTO.builder()
                        .type(TypeAlerte.INTERACTION)
                        .medicament_id(interaction.getMedicament_a().getId())
                        .medicament_associe_id(interaction.getMedicament_b().getId())
                        .gravite(interaction.getGravite())
                        .bloquante(interaction.estBloquante()) // true seulement si CONTRE_INDIQUEE
                        .message(interaction.getMedicament_a().getNom_commercial() + " + "
                                + interaction.getMedicament_b().getNom_commercial() + " : "
                                + interaction.getDescription())
                        .build());
            }
        }

        // compatible = true si AUCUNE alerte n'est bloquante (une alerte GRAVE seule reste compatible)
        boolean compatible = alertes.stream().noneMatch(AlerteDTO::isBloquante);
        return new VerificationResponse(compatible, alertes);
    }

    // DELETE + MÉTIER (RG6) : retirer un médicament du catalogue.
    // @Transactional : si une étape échoue, rien n'est supprimé (tout ou rien).
    @Override
    @Transactional
    public void retirerMedicament(Long id, String motif) {
        // 1) Le médicament existe ? Sinon on lance l'exception -> le GlobalExceptionHandler renvoie 404
        Medicament medicament = medicamentRepository.findById(id)
                .orElseThrow(() -> new MedicamentNotFoundException(id));

        // 2) Appel SYNCHRONE à ordonnance-service (Feign) : y a-t-il des ordonnances actives
        //    avec ce médicament ? Si ordonnance-service est en panne, le circuit breaker
        //    utilise le fallback qui renvoie une liste vide -> le retrait continue.
        List<OrdonnanceDTO> ordonnancesActives = ordonnanceClient.getOrdonnances(id, statutActif);
        if (!ordonnancesActives.isEmpty()) {
            // On ne bloque pas : on trace un avertissement dans les logs
            log.warn("Retrait de {} : {} ordonnance(s) active(s) concernée(s) {}",
                    medicament.getNom_commercial(), ordonnancesActives.size(),
                    ordonnancesActives.stream().map(OrdonnanceDTO::getId).toList());
        }

        // 3) On supprime d'abord ses interactions (clé étrangère), puis le médicament
        interactionRepository.deleteByMedicamentId(id);
        medicamentRepository.delete(medicament);

        // 4) Communication ASYNCHRONE : on prévient les autres services par RabbitMQ
        //    (pharmacie-service bloquera la délivrance de ce médicament)
        String motifFinal = (motif == null || motif.isBlank()) ? MOTIF_PAR_DEFAUT : motif;
        eventPublisher.publierMedicamentRetire(new MedicamentRetireEvent(id, motifFinal, LocalDateTime.now()));
    }
}
