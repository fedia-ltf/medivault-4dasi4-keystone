package tn.esprit.medivault.soindomicile.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}") private String exchange;
    @Value("${medivault.rabbitmq.queue.medicament-retire}") private String queue;
    @Value("${medivault.rabbitmq.routing-key.medicament-retire}") private String cleRetrait;

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);   // durable
    }

    @Bean
    public Queue medicamentRetireQueue() {
        return QueueBuilder.durable(queue).build();        // ma propre file durable
    }

    @Bean
    public Binding medicamentRetireBinding() {
        return BindingBuilder.bind(medicamentRetireQueue()).to(medivaultExchange()).with(cleRetrait);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();          // les messages circulent en JSON
    }
}
