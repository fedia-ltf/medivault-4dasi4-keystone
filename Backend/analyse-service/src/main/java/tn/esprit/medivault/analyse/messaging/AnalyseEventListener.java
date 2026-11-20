package tn.esprit.medivault.analyse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.analyse.event.ConsultationAjouteeEvent;
import tn.esprit.medivault.analyse.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.analyse.event.RendezVousConfirmeEvent;
import tn.esprit.medivault.analyse.service.IAnalyseService;

/**
 * Consommateurs RabbitMQ : une file durable par événement, propre à analyse-service.
 * Le message est acquitté après traitement ; un service arrêté retrouve ses messages au redémarrage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyseEventListener {

    private final IAnalyseService analyseService;

    @RabbitListener(queues = "${medivault.rabbitmq.queues.consultation-ajoutee}")
    public void onConsultationAjoutee(ConsultationAjouteeEvent event) {
        log.info("ConsultationAjoutee reçu : {}", event);
        analyseService.creerDepuisConsultation(event);
    }

    @RabbitListener(queues = "${medivault.rabbitmq.queues.rendezvous-confirme}")
    public void onRendezVousConfirme(RendezVousConfirmeEvent event) {
        log.info("RendezVousConfirme reçu : {}", event);
        analyseService.affecterLaboratoire(event);
    }

    @RabbitListener(queues = "${medivault.rabbitmq.queues.rendezvous-annule}")
    public void onRendezVousAnnule(RendezVousAnnuleEvent event) {
        log.info("RendezVousAnnule reçu : {}", event);
        analyseService.annulerAffectation(event);
    }
}
