package tn.esprit.medivault.medicament.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.MedicamentRetireEvent;

// PUBLIER un événement sur RabbitMQ = communication ASYNCHRONE.
// On poste le message et on continue sans attendre de réponse.
@Component
@RequiredArgsConstructor
@Slf4j
public class MedicamentEventPublisher {

    // RabbitTemplate : l'outil Spring pour envoyer des messages à RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    // Valeurs lues dans application.properties :
    // exchange = "medivault.events" (la boîte aux lettres commune)
    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    // clé de routage = "medicament.retire" (l'étiquette du message)
    @Value("${medivault.rabbitmq.routing-key.medicament-retire}")
    private String routingKeyMedicamentRetire;

    public void publierMedicamentRetire(MedicamentRetireEvent event) {
        try {
            // Envoie l'événement (converti en JSON) dans l'exchange avec la clé "medicament.retire".
            // RabbitMQ le distribue aux files abonnées à cette clé (ex. celle de pharmacie-service).
            rabbitTemplate.convertAndSend(exchange, routingKeyMedicamentRetire, event);
            log.info("Événement MedicamentRetire publié : {}", event);
        } catch (AmqpException e) {
            // RabbitMQ arrêté : le retrait reste valide, on trace simplement l'échec
            log.error("Échec de publication de MedicamentRetire ({}) : {}", event, e.getMessage());
        }
    }
}
