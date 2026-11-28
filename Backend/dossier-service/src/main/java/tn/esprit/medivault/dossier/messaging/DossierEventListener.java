package tn.esprit.medivault.dossier.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.dossier.config.RabbitConfig;
import tn.esprit.medivault.dossier.entities.TypeLien;
import tn.esprit.medivault.dossier.messaging.events.*;
import tn.esprit.medivault.dossier.services.DossierService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DossierEventListener {

    private final DossierService dossierService;

    @RabbitListener(queues = RabbitConfig.QUEUE_UTILISATEUR_CREE)
    public void onUtilisateurCree(UtilisateurCree event) {
        log.info("Événement UtilisateurCree reçu : utilisateurId={}, role={}, nom={}, prenom={}",
                event.utilisateurId(), event.role(), event.nom(), event.prenom());
        if (event.role() != null && "PATIENT".equalsIgnoreCase(event.role().trim())) {
            dossierService.creerDossierVide(event.utilisateurId());
            log.info("Dossier médical initialisé pour le patient : {}", event.utilisateurId());
        } else {
            log.info("Événement UtilisateurCree ignoré pour le rôle non-patient : {}", event.role());
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_RENDEZVOUS_CONFIRME)
    public void onRendezVousConfirme(RendezVousConfirme event) {
        log.info("Événement RendezVousConfirme reçu : rendezVousId={}, patientId={}, professionnelId={}, type={}",
                event.rendezVousId(), event.patientId(), event.professionnelId(), event.typeProfessionnel());
        dossierService.accorderAcces(event.patientId(), event.professionnelId());
        log.info("Accès ACTIF accordé au professionnel {} sur le dossier du patient {}",
                event.professionnelId(), event.patientId());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_RENDEZVOUS_ANNULE)
    public void onRendezVousAnnule(RendezVousAnnule event) {
        log.info("Événement RendezVousAnnule reçu : rendezVousId={}, patientId={}, professionnelId={}, type={}",
                event.rendezVousId(), event.patientId(), event.professionnelId(), event.typeProfessionnel());
        dossierService.retirerAccesParPatient(event.patientId(), event.professionnelId());
        log.info("Accès RETIRE pour le professionnel {} sur le dossier du patient {}",
                event.professionnelId(), event.patientId());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ORDONNANCE_CREEE)
    public void onOrdonnanceCreee(OrdonnanceCreee event) {
        log.info("Événement OrdonnanceCreee reçu : ordonnanceId={}, code={}, patientId={}, medecinId={}, dateEmission={}",
                event.ordonnanceId(), event.code(), event.patientId(), event.medecinId(), event.dateEmission());
        LocalDateTime date = event.dateEmission() != null ? event.dateEmission().atStartOfDay() : null;
        String libelle = "Ordonnance " + event.code();
        dossierService.ajouterLien(
                event.patientId(),
                TypeLien.ORDONNANCE,
                event.code(),
                date,
                event.medecinId(),
                libelle
        );
        log.info("Lien ORDONNANCE ajouté au dossier du patient {} pour le code {}",
                event.patientId(), event.code());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_RESULTAT_DISPONIBLE)
    public void onResultatDisponible(ResultatDisponible event) {
        log.info("Événement ResultatDisponible reçu : analyseId={}, patientId={}, medecinId={}, laboratoireId={}, typeAnalyse={}, date={}",
                event.analyseId(), event.patientId(), event.medecinId(), event.laboratoireId(), event.typeAnalyse(), event.date());
        String libelle = "Résultat " + event.typeAnalyse();
        dossierService.ajouterLien(
                event.patientId(),
                TypeLien.RESULTAT_ANALYSE,
                event.analyseId(),
                event.date(),
                event.laboratoireId(),
                libelle
        );
        log.info("Lien RESULTAT_ANALYSE ajouté au dossier du patient {} pour l'analyse {}",
                event.patientId(), event.analyseId());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_SOIN_REALISE)
    public void onSoinRealise(SoinRealise event) {
        log.info("Événement SoinRealise reçu : visiteId={}, patientId={}, typeSoin={}, dateRapport={}",
                event.visiteId(), event.patientId(), event.typeSoin(), event.dateRapport());
        String libelle = "Soin " + event.typeSoin();
        dossierService.ajouterLien(
                event.patientId(),
                TypeLien.SOIN,
                event.visiteId(),
                event.dateRapport(),
                null,
                libelle
        );
        log.info("Lien SOIN ajouté au dossier du patient {} pour la visite {}",
                event.patientId(), event.visiteId());
    }
}
