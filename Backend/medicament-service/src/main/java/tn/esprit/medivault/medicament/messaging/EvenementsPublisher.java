package tn.esprit.medivault.medicament.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.medivault.medicament.events.SanctionPharmacie;
import tn.esprit.medivault.medicament.events.VenteSensibleEnregistree;

// PUBLIER un événement sur RabbitMQ = communication ASYNCHRONE : on poste le message et on continue.
// Les deux événements de la documentation, sur l'exchange commun "medivault.events" :
//   VenteSensibleEnregistree  (vente.sensible.enregistree) -> ordonnance-service
//   SanctionPharmacie         (pharmacie.sanction)         -> notification-service
// AFTER_COMMIT : le message part seulement une fois la vente (ou l'avertissement) réellement enregistrée en base.
@Component
@RequiredArgsConstructor
@Slf4j
public class EvenementsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.routing-key.vente-sensible-enregistree}")
    private String cleVente;

    @Value("${medivault.rabbitmq.routing-key.sanction-pharmacie}")
    private String cleSanction;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void surVente(VenteSensibleEnregistree evenement) {
        publier(cleVente, evenement);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void surSanction(SanctionPharmacie evenement) {
        publier(cleSanction, evenement);
    }

    private void publier(String cle, Object evenement) {
        try {
            rabbitTemplate.convertAndSend(exchange, cle, evenement);
            log.info("Événement publié ({}) : {}", cle, evenement);
        } catch (AmqpException e) {
            // RabbitMQ arrêté : la vente reste valide, on trace simplement l'échec
            log.error("Échec de publication de l'événement {} ({}) : {}", cle, evenement, e.getMessage());
        }
    }
}