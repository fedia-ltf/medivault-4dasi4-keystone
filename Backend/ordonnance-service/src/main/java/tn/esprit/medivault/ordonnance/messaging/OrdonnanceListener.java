package tn.esprit.medivault.ordonnance.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrdonnanceListener {
    private final OrdonnanceService ordonnanceService;

    public OrdonnanceListener(OrdonnanceService ordonnanceService) {
        this.ordonnanceService = ordonnanceService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_DELIVREE)
    public void onOrdonnanceDelivree(OrdonnanceDelivreeEvent event) {
        ordonnanceService.marquerDelivree(event.getOrdonnanceId());
    }
}