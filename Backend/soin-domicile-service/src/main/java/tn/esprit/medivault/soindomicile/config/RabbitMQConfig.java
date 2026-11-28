package tn.esprit.medivault.soindomicile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * soin-domicile publie SoinRealise (soin.realise -> dossier) et AlerteMesure (mesure.alerte -> notification).
 * Il n'écoute plus medicament.retire : cet événement n'est plus publié (documentation finale v4).
 */
@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}") private String exchange;

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);   // durable
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);   // JSON, dates ISO (ObjectMapper de Spring)
    }
}
