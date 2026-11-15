package tn.esprit.medivault.medicament.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;
import tn.esprit.medivault.medicament.services.IConsommationService;

// RECEVOIR un événement de RabbitMQ (communication ASYNCHRONE).
// Quand pharmacie-service publie "OrdonnanceDelivree", le message arrive dans
// notre file "medicament-service.ordonnance-delivree" et cette méthode est appelée toute seule.
@Component
@RequiredArgsConstructor
@Slf4j
public class OrdonnanceDelivreeListener {

    private final IConsommationService consommationService;

    // @RabbitListener : écoute la file dont le nom est dans application.properties.
    // Le JSON reçu est transformé en OrdonnanceDelivreeEvent (grâce au converter de RabbitMQConfig).
    // Si la méthode se termine sans erreur, Spring confirme le message (ack) -> il est retiré de la file.
    @RabbitListener(queues = "${medivault.rabbitmq.queue.ordonnance-delivree}")
    public void onOrdonnanceDelivree(OrdonnanceDelivreeEvent event) {
        log.info("Événement OrdonnanceDelivree reçu : {}", event);
        consommationService.enregistrerDelivrance(event);
    }
}
