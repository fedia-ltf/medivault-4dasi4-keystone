package tn.esprit.medivault.soindomicile.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.soindomicile.dto.AlerteMesureEvent;
import tn.esprit.medivault.soindomicile.dto.SoinRealiseEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class SoinEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${medivault.rabbitmq.exchange}") private String exchange;
    @Value("${medivault.rabbitmq.routing-key.soin-realise}") private String cleSoin;
    @Value("${medivault.rabbitmq.routing-key.alerte-mesure}") private String cleAlerte;

    public void publierSoinRealise(SoinRealiseEvent e) {
        rabbitTemplate.convertAndSend(exchange, cleSoin, e);
        log.info("SoinRealise publié : {}", e);
    }

    public void publierAlerteMesure(AlerteMesureEvent e) {
        rabbitTemplate.convertAndSend(exchange, cleAlerte, e);
        log.info("AlerteMesure publiée : {}", e);
    }
}
