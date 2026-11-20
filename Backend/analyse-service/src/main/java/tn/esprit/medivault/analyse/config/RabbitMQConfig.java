package tn.esprit.medivault.analyse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange topic commun medivault.events ; analyse-service possède ses propres files durables :
 * un message reste dans la file tant que le service est arrêté et il est traité à son redémarrage.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.queues.consultation-ajoutee}")
    private String consultationQueue;

    @Value("${medivault.rabbitmq.queues.rendezvous-confirme}")
    private String rendezVousConfirmeQueue;

    @Value("${medivault.rabbitmq.queues.rendezvous-annule}")
    private String rendezVousAnnuleQueue;

    @Value("${medivault.rabbitmq.routing-keys.consultation-ajoutee}")
    private String consultationKey;

    @Value("${medivault.rabbitmq.routing-keys.rendezvous-confirme}")
    private String rendezVousConfirmeKey;

    @Value("${medivault.rabbitmq.routing-keys.rendezvous-annule}")
    private String rendezVousAnnuleKey;

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue consultationAjouteeQueue() {
        return QueueBuilder.durable(consultationQueue).build();
    }

    @Bean
    public Queue rendezVousConfirmeQueue() {
        return QueueBuilder.durable(rendezVousConfirmeQueue).build();
    }

    @Bean
    public Queue rendezVousAnnuleQueue() {
        return QueueBuilder.durable(rendezVousAnnuleQueue).build();
    }

    @Bean
    public Binding consultationAjouteeBinding() {
        return BindingBuilder.bind(consultationAjouteeQueue()).to(medivaultExchange()).with(consultationKey);
    }

    @Bean
    public Binding rendezVousConfirmeBinding() {
        return BindingBuilder.bind(rendezVousConfirmeQueue()).to(medivaultExchange()).with(rendezVousConfirmeKey);
    }

    @Bean
    public Binding rendezVousAnnuleBinding() {
        return BindingBuilder.bind(rendezVousAnnuleQueue()).to(medivaultExchange()).with(rendezVousAnnuleKey);
    }

    /**
     * Événements en JSON. Le type Java est déduit du paramètre du listener (INFERRED) :
     * l'en-tête __TypeId__ envoyé par rendezvous ou dossier désigne leurs propres classes, absentes ici.
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
