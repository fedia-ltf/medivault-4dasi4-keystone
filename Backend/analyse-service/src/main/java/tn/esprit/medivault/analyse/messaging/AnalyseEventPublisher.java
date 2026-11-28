package tn.esprit.medivault.analyse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.medivault.analyse.event.ResultatDisponibleEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyseEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.routing-keys.resultat-disponible}")
    private String routingKeyResultatDisponible;

    /** Publié seulement après l'enregistrement (commit) : jamais d'événement pour un résultat non sauvegardé. */
    public void publierResultatDisponible(ResultatDisponibleEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoyer(event);
                }
            });
        } else {
            envoyer(event);
        }
    }

    private void envoyer(ResultatDisponibleEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyResultatDisponible, event);
            log.info("Événement ResultatDisponible publié : {}", event);
        } catch (AmqpException e) {
            // RabbitMQ arrêté : le résultat reste enregistré, on trace l'échec
            log.error("Échec de publication de ResultatDisponible ({}) : {}", event, e.getMessage());
        }
    }
}
