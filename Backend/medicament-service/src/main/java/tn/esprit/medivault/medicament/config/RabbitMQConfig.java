package tn.esprit.medivault.medicament.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration de RabbitMQ. medicament-service PUBLIE deux événements et n'en consomme aucun
// (documentation) : il n'a donc besoin d'aucune file, seulement de l'exchange commun.
@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    // Exchange commun à tous les microservices MediVault. Type "topic" : il distribue selon la clé de routage.
    // true = durable (survit à un redémarrage de RabbitMQ), false = pas supprimé automatiquement.
    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

    // Messages en JSON (lisible par tous les services, qui n'ont pas les mêmes classes Java)
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}