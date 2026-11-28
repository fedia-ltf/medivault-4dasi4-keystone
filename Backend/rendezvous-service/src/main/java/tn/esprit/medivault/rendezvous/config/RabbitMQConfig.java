package tn.esprit.medivault.rendezvous.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour rendezvous-service.
 * Consomme consultation.ajoutee pour clôturer les rendez-vous terminés.
 * Publie rendezvous.confirme vers user-service et analyse-service.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.queues.consultation-ajoutee}")
    private String consultationQueue;

    @Value("${medivault.rabbitmq.routing-keys.consultation-ajoutee}")
    private String consultationKey;

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue consultationAjouteeQueue() {
        return QueueBuilder.durable(consultationQueue).build();
    }

    @Bean
    public Binding consultationAjouteeBinding() {
        return BindingBuilder.bind(consultationAjouteeQueue()).to(medivaultExchange()).with(consultationKey);
    }

    /**
     * JSON avec type déduit du paramètre du listener (INFERRED) : l'en-tête __TypeId__ envoyé
     * par dossier-service désigne sa propre classe, absente ici (sinon le message est rejeté).
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}

