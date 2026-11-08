package tn.esprit.medivault.analyse.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
    private String rendezVousQueue;

    @Value("${medivault.rabbitmq.routing-keys.consultation-ajoutee}")
    private String consultationKey;

    @Value("${medivault.rabbitmq.routing-keys.rendezvous-confirme}")
    private String rendezVousKey;

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
        return QueueBuilder.durable(rendezVousQueue).build();
    }

    @Bean
    public Binding consultationAjouteeBinding() {
        return BindingBuilder.bind(consultationAjouteeQueue()).to(medivaultExchange()).with(consultationKey);
    }

    @Bean
    public Binding rendezVousConfirmeBinding() {
        return BindingBuilder.bind(rendezVousConfirmeQueue()).to(medivaultExchange()).with(rendezVousKey);
    }

    /** Les événements circulent en JSON. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
