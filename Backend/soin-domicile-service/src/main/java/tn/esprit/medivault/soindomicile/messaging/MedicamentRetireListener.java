package tn.esprit.medivault.soindomicile.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.soindomicile.dto.MedicamentRetireEvent;
import tn.esprit.medivault.soindomicile.services.IVisiteService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicamentRetireListener {

    private final IVisiteService visiteService;

    @RabbitListener(queues = "${medivault.rabbitmq.queue.medicament-retire}")
    public void onMedicamentRetire(MedicamentRetireEvent e) {
        int n = visiteService.annulerInjections(e.medicamentId(), e.motif());   // RG8
        log.info("MedicamentRetire {} reçu : {} injection(s) annulée(s)", e.medicamentId(), n);
    }
}
