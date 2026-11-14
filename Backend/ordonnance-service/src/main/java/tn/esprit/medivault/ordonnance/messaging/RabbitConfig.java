package tn.esprit.medivault.ordonnance.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "medivault.events";
    public static final String QUEUE_DELIVREE = "ordonnance.delivree.queue";

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue ordonnanceDelivreeQueue() {
        return QueueBuilder.durable(QUEUE_DELIVREE).build();
    }

    @Bean
    public Binding ordonnanceDelivreeBinding(Queue ordonnanceDelivreeQueue, TopicExchange medivaultExchange) {
        return BindingBuilder.bind(ordonnanceDelivreeQueue).to(medivaultExchange).with("ordonnance.delivree");
    }
}