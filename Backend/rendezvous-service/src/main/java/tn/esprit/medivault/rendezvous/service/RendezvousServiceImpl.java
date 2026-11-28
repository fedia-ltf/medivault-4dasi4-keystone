package tn.esprit.medivault.rendezvous.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.rendezvous.client.AnalyseClient;
import tn.esprit.medivault.rendezvous.client.UserClient;
import tn.esprit.medivault.rendezvous.dto.*;
import tn.esprit.medivault.rendezvous.entity.Creneau;
import tn.esprit.medivault.rendezvous.entity.RendezVous;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;
import tn.esprit.medivault.rendezvous.enums.TypeProfessionnel;
import tn.esprit.medivault.rendezvous.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.rendezvous.event.RendezVousConfirmeEvent;
import tn.esprit.medivault.rendezvous.messaging.RendezvousEventPublisher;
import tn.esprit.medivault.rendezvous.repository.CreneauRepository;
import tn.esprit.medivault.rendezvous.repository.RendezVousRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RendezvousServiceImpl implements IRendezvousService {

    private final CreneauRepository creneauRepository;
    private final RendezVousRepository rendezVousRepository;
    private final UserClient userClient;
    private final AnalyseClient analyseClient;
    private final RendezvousEventPublisher eventPublisher;

    @Override
    @Transactional
    public List<CreneauResponse> genererCreneaux(String professionnelId) {
        log.info("Appel synchrone OpenFeign vers user-service pour le professionnel {}", professionnelId);
        DisponibilitesResponse dispo = userClient.getDisponibilites(professionnelId);
        if (dispo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel introuvable");
        }

        TypeProfessionnel type = TypeProfessionnel.MEDECIN;
        if (dispo.getTypeProfessionnel() != null) {
            String tp = dispo.getTypeProfessionnel().toUpperCase();
            if (tp.contains("LABO")) {
                type = TypeProfessionnel.LABORANTIN;
            } else {
                type = TypeProfessionnel.MEDECIN;
            }
        }

        LocalTime heureDebut = dispo.getHeureDebut() != null ? dispo.getHeureDebut() : LocalTime.of(9, 0);
        LocalTime heureFin = dispo.getHeureFin() != null ? dispo.getHeureFin() : LocalTime.of(17, 0);
        List<String> jours = dispo.getJoursTravail() != null ? dispo.getJoursTravail() : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

        List<Creneau> nouveauxCreneaux = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Générer les créneaux pour les 7 prochains jours
        for (int i = 1; i <= 7; i++) {
            LocalDate date = today.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (jours.stream().anyMatch(j -> j.equalsIgnoreCase(dow.name()))) {
                LocalTime current = heureDebut;
                while (current.isBefore(heureFin)) {
                    LocalDateTime start = LocalDateTime.of(date, current);
                    LocalDateTime end = start.plusMinutes(30);
                    nouveauxCreneaux.add(Creneau.builder()
                            .professionnelId(professionnelId)
                            .typeProfessionnel(type)
                            .dateDebut(start)
                            .dateFin(end)
                            .build());
                    current = current.plusMinutes(30);
                }
            }
        }

        List<Creneau> saved = creneauRepository.saveAll(nouveauxCreneaux);
        log.info("{} créneaux générés pour le professionnel {}", saved.size(), professionnelId);
        return saved.stream().map(this::mapToCreneauResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreneauResponse> getCreneauxLibres(String professionnelId, LocalDate date) {
        List<Creneau> creneaux;
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            creneaux = creneauRepository.findByProfessionnelIdAndDate(professionnelId, start, end);
        } else {
            creneaux = creneauRepository.findByProfessionnelId(professionnelId);
        }

        return creneaux.stream()
                .filter(Creneau::estLibre)
                .map(this::mapToCreneauResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RendezVousResponse prendreRendezVous(RendezVousRequest request) {
        Creneau creneau = creneauRepository.findById(request.getCreneauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));

        // RG8 : Vérification de la disponibilité du créneau
        if (!creneau.estLibre()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce créneau est déjà réservé");
        }

        Long analyseId = null;

        // Contrôles spécifiques pour un rendez-vous au laboratoire (Page 15)
        if (creneau.getTypeProfessionnel() == TypeProfessionnel.LABORANTIN) {
            if (request.getAnalyseId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "L'analyseId est obligatoire pour réserver un rendez-vous au laboratoire");
            }

            // Appel synchrone OpenFeign vers analyse-service (GET /analyses/{id})
            AnalyseResponse analyse = analyseClient.getAnalyseById(request.getAnalyseId());
            if (analyse == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La prescription d'analyse n'existe pas");
            }

            // Contrôle 2 : Propriétaire de l'analyse
            if (analyse.getPatientId() != null && !analyse.getPatientId().equals(request.getPatientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "L'analyse n'appartient pas au patient qui effectue la réservation");
            }

            // Contrôle 3 : Statut de l'analyse (doit être EN_ATTENTE)
            if (analyse.getStatut() != null && !"EN_ATTENTE".equalsIgnoreCase(analyse.getStatut())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "L'analyse n'est pas au statut EN_ATTENTE");
            }

            analyseId = request.getAnalyseId();
        }

        RendezVous rdv = RendezVous.builder()
                .patientId(request.getPatientId())
                .motif(request.getMotif())
                .statut(StatutRdv.CONFIRME)
                .analyseId(analyseId)
                .creneau(creneau)
                .build();

        creneau.setRendezVous(rdv);
        RendezVous saved = rendezVousRepository.save(rdv);

        // Publication de l'événement RendezVousConfirme (dossier, analyse, notification)
        RendezVousConfirmeEvent event = new RendezVousConfirmeEvent(
                saved.getId(),
                saved.getPatientId(),
                creneau.getProfessionnelId(),
                creneau.getTypeProfessionnel().name(),
                saved.getAnalyseId(),
                creneau.getDateDebut()
        );
        eventPublisher.publierRendezVousConfirme(event);

        return mapToRendezVousResponse(saved);
    }

    @Override
    @Transactional
    public RendezVousResponse annulerRendezVous(Long id) {
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));

        if (rdv.getStatut() == StatutRdv.ANNULE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le rendez-vous est déjà annulé");
        }

        rdv.annuler();
        RendezVous saved = rendezVousRepository.save(rdv);
        Creneau creneau = saved.getCreneau();

        // Publication de l'événement RendezVousAnnule (repasse l'analyse EN_ATTENTE, accès RETIRE)
        RendezVousAnnuleEvent annuleEvent = new RendezVousAnnuleEvent(
                saved.getId(),
                saved.getPatientId(),
                creneau != null ? creneau.getProfessionnelId() : null,
                creneau != null ? creneau.getTypeProfessionnel().name() : null,
                saved.getAnalyseId()
        );
        eventPublisher.publierRendezVousAnnule(annuleEvent);

        log.info("Rendez-vous {} annulé et événement RendezVousAnnule publié", id);
        return mapToRendezVousResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreneauResponse getCreneauById(Long id) {
        Creneau creneau = creneauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        return mapToCreneauResponse(creneau);
    }

    @Override
    @Transactional
    public CreneauResponse creerCreneau(CreneauRequest request) {
        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit être postérieure à la date de début");
        }
        Creneau c = Creneau.builder()
                .professionnelId(request.getProfessionnelId())
                .typeProfessionnel(request.getTypeProfessionnel() != null ? request.getTypeProfessionnel() : TypeProfessionnel.MEDECIN)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .build();
        Creneau saved = creneauRepository.save(c);
        log.info("Créneau {} créé manuellement pour le professionnel {}", saved.getId(), saved.getProfessionnelId());
        return mapToCreneauResponse(saved);
    }

    @Override
    @Transactional
    public CreneauResponse modifierCreneau(Long id, CreneauRequest request) {
        Creneau c = creneauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        if (!c.estLibre()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de modifier un créneau déjà réservé");
        }
        if (request.getDateDebut() != null) c.setDateDebut(request.getDateDebut());
        if (request.getDateFin() != null) c.setDateFin(request.getDateFin());
        Creneau updated = creneauRepository.save(c);
        log.info("Créneau {} modifié avec succès", id);
        return mapToCreneauResponse(updated);
    }

    @Override
    @Transactional
    public void supprimerCreneau(Long id) {
        Creneau creneau = creneauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        if (!creneau.estLibre()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer un créneau déjà réservé");
        }
        creneauRepository.delete(creneau);
        log.info("Créneau {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RendezVousResponse getRendezVousById(Long id) {
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
        return mapToRendezVousResponse(rdv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendezVousResponse> getAllRendezVous() {
        return rendezVousRepository.findAll().stream()
                .map(this::mapToRendezVousResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendezVousResponse> getRendezVousByPatient(String patientId) {
        return rendezVousRepository.findByPatientIdOrderByCreneauDateDebutDesc(patientId).stream()
                .map(this::mapToRendezVousResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendezVousResponse> getRendezVousByProfessionnel(String professionnelId) {
        return rendezVousRepository.findByCreneauProfessionnelIdOrderByCreneauDateDebutDesc(professionnelId).stream()
                .map(this::mapToRendezVousResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void supprimerRendezVous(Long id) {
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));

        Creneau creneau = rdv.getCreneau();
        if (creneau != null) {
            creneau.setRendezVous(null);
            creneauRepository.save(creneau);
        }

        rendezVousRepository.delete(rdv);
        log.info("Rendez-vous {} supprimé avec succès", id);
    }

    @Override
    @Transactional
    public void traiterConsultationAjoutee(String patientId, String medecinId) {
        rendezVousRepository.findTopByPatientIdAndCreneauProfessionnelIdAndStatutOrderByCreneauDateDebutDesc(
                patientId, medecinId, StatutRdv.CONFIRME
        ).ifPresent(rdv -> {
            rdv.terminer();
            rendezVousRepository.save(rdv);
            log.info("Rendez-vous {} clôturé (TERMINE) suite à la consultation du patient {} chez le médecin {}",
                    rdv.getId(), patientId, medecinId);
        });
    }

    private CreneauResponse mapToCreneauResponse(Creneau c) {
        return CreneauResponse.builder()
                .id(c.getId())
                .professionnelId(c.getProfessionnelId())
                .typeProfessionnel(c.getTypeProfessionnel())
                .dateDebut(c.getDateDebut())
                .dateFin(c.getDateFin())
                .libre(c.estLibre())
                .build();
    }

    private RendezVousResponse mapToRendezVousResponse(RendezVous rdv) {
        Creneau c = rdv.getCreneau();
        return RendezVousResponse.builder()
                .id(rdv.getId())
                .patientId(rdv.getPatientId())
                .motif(rdv.getMotif())
                .statut(rdv.getStatut())
                .analyseId(rdv.getAnalyseId())
                .creneauId(c != null ? c.getId() : null)
                .professionnelId(c != null ? c.getProfessionnelId() : null)
                .typeProfessionnel(c != null ? c.getTypeProfessionnel() : null)
                .dateDebut(c != null ? c.getDateDebut() : null)
                .dateFin(c != null ? c.getDateFin() : null)
                .build();
    }
}
