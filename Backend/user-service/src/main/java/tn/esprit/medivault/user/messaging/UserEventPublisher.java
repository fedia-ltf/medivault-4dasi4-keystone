package tn.esprit.medivault.user.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.user.dto.UtilisateurCreeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.routing-key.utilisateur-cree}")
    private String routingKeyUtilisateurCree;

    public void publierUtilisateurCree(UtilisateurCreeEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyUtilisateurCree, event);
            log.info("Événement UtilisateurCree publié : {}", event);
        } catch (AmqpException e) {
            // RabbitMQ arrêté : le compte reste créé, on trace l'échec
            log.error("Échec de publication de UtilisateurCree ({}) : {}", event, e.getMessage());
        }
    }
}
