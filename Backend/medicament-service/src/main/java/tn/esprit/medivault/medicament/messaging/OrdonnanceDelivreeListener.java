package tn.esprit.medivault.medicament.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;
import tn.esprit.medivault.medicament.services.IConsommationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdonnanceDelivreeListener {

    private final IConsommationService consommationService;

    @RabbitListener(queues = "${medivault.rabbitmq.queue.ordonnance-delivree}")
    public void onOrdonnanceDelivree(OrdonnanceDelivreeEvent event) {
        log.info("Événement OrdonnanceDelivree reçu : {}", event);
        consommationService.enregistrerDelivrance(event);
    }
}
