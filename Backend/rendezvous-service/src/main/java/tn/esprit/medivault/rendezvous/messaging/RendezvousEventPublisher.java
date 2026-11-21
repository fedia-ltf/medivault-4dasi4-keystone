package tn.esprit.medivault.rendezvous.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.rendezvous.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.rendezvous.event.RendezVousConfirmeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class RendezvousEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.routing-keys.rendezvous-confirme}")
    private String routingKeyRendezVousConfirme;

    @Value("${medivault.rabbitmq.routing-keys.rendezvous-annule}")
    private String routingKeyRendezVousAnnule;

    public void publierRendezVousConfirme(RendezVousConfirmeEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyRendezVousConfirme, event);
            log.info("Événement RendezVousConfirme publié sur {} avec la clé {} : {}",
                    exchange, routingKeyRendezVousConfirme, event);
        } catch (AmqpException e) {
            log.error("Échec de publication de RendezVousConfirme ({}) : {}", event, e.getMessage());
        }
    }

    public void publierRendezVousAnnule(RendezVousAnnuleEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyRendezVousAnnule, event);
            log.info("Événement RendezVousAnnule publié sur {} avec la clé {} : {}",
                    exchange, routingKeyRendezVousAnnule, event);
        } catch (AmqpException e) {
            log.error("Échec de publication de RendezVousAnnule ({}) : {}", event, e.getMessage());
        }
    }
}
