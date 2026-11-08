package tn.esprit.medivault.medicament.config;

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

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.queue.ordonnance-delivree}")
    private String queueOrdonnanceDelivree;

    @Value("${medivault.rabbitmq.routing-key.ordonnance-delivree}")
    private String routingKeyOrdonnanceDelivree;

    // Exchange commun à tous les microservices MediVault
    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue ordonnanceDelivreeQueue() {
        return QueueBuilder.durable(queueOrdonnanceDelivree).build();
    }

    @Bean
    public Binding ordonnanceDelivreeBinding(Queue ordonnanceDelivreeQueue, TopicExchange medivaultExchange) {
        return BindingBuilder.bind(ordonnanceDelivreeQueue).to(medivaultExchange).with(routingKeyOrdonnanceDelivree);
    }

    // Messages en JSON ; le type est déduit du paramètre du listener
    // (les autres services n'ont pas les mêmes classes Java)
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
