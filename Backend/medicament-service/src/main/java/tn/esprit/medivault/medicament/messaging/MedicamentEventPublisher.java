package tn.esprit.medivault.medicament.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.MedicamentRetireEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicamentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.routing-key.medicament-retire}")
    private String routingKeyMedicamentRetire;

    public void publierMedicamentRetire(MedicamentRetireEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyMedicamentRetire, event);
            log.info("Événement MedicamentRetire publié : {}", event);
        } catch (AmqpException e) {
            // RabbitMQ arrêté : le retrait reste valide, on trace simplement l'échec
            log.error("Échec de publication de MedicamentRetire ({}) : {}", event, e.getMessage());
        }
    }
}
