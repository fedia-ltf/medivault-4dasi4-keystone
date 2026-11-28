package tn.esprit.medivault.dossier.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.dossier.config.RabbitConfig;
import tn.esprit.medivault.dossier.messaging.events.ConsultationAjouteeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class DossierEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publierConsultationAjoutee(ConsultationAjouteeEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    "consultation.ajoutee",
                    event
            );
            log.info("Événement ConsultationAjoutee publié : consultationId={}, patientId={}, medecinId={}",
                    event.consultationId(), event.patientId(), event.medecinId());
        } catch (AmqpException ex) {
            log.error("Échec de la publication de l'événement ConsultationAjoutee (consultationId={}) : {}",
                    event.consultationId(), ex.getMessage());
        }
    }
}
