package tn.esprit.medivault.rendezvous.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.rendezvous.event.ConsultationAjouteeEvent;
import tn.esprit.medivault.rendezvous.service.IRendezvousService;

/**
 * Consomme l'événement ConsultationAjoutee publié par dossier-service.
 * Clôture le rendez-vous lié au patient et au médecin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationAjouteeListener {

    private final IRendezvousService rendezvousService;

    @RabbitListener(queues = "${medivault.rabbitmq.queues.consultation-ajoutee}")
    public void onConsultationAjoutee(ConsultationAjouteeEvent event) {
        log.info("Événement ConsultationAjoutee reçu : consultationId={}, patientId={}, medecinId={}",
                event.consultationId(), event.patientId(), event.medecinId());
        rendezvousService.traiterConsultationAjoutee(event.patientId(), event.medecinId());
    }
}

