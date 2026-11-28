package tn.esprit.medivault.ordonnance.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.ordonnance.services.OrdonnanceService;

@Component
@Slf4j
public class OrdonnanceListener {

    private final OrdonnanceService ordonnanceService;

    public OrdonnanceListener(OrdonnanceService ordonnanceService) {
        this.ordonnanceService = ordonnanceService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_VENTE_SENSIBLE)
    public void onVenteSensibleEnregistree(VenteSensibleEnregistreeEvent event) {
        log.info("VenteSensibleEnregistree reçue : ordonnanceId={}, medicamentId={}, quantite={}",
                event.getOrdonnanceId(), event.getMedicamentId(), event.getQuantite());
        ordonnanceService.appliquerVente(event);
    }
}
