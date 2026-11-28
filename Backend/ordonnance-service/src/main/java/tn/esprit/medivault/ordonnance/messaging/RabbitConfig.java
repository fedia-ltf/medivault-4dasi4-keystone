package tn.esprit.medivault.ordonnance.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "medivault.events";
    public static final String QUEUE_VENTE_SENSIBLE = "ordonnance.vente-sensible-enregistree";
    public static final String ROUTING_KEY_VENTE_SENSIBLE = "vente.sensible.enregistree";

    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue venteSensibleQueue() {
        return QueueBuilder.durable(QUEUE_VENTE_SENSIBLE).build();
    }

    @Bean
    public Binding venteSensibleBinding(Queue venteSensibleQueue, TopicExchange medivaultExchange) {
        return BindingBuilder.bind(venteSensibleQueue).to(medivaultExchange).with(ROUTING_KEY_VENTE_SENSIBLE);
    }

    /**
     * JSON entre services : le type est déduit du paramètre du listener (INFERRED), pas du nom
     * de classe Java de l'émetteur, comme dans les autres services.
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
