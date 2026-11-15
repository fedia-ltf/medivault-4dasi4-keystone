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

// Configuration de RabbitMQ. Au démarrage, Spring crée dans RabbitMQ :
//   1) l'exchange "medivault.events" (la boîte aux lettres commune)
//   2) notre file "medicament-service.ordonnance-delivree"
//   3) le lien (binding) : "les messages avec la clé ordonnance.delivree vont dans notre file"
// Chaque @Bean = un objet créé et géré par Spring.
@Configuration
public class RabbitMQConfig {

    // Noms lus dans application.properties
    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    @Value("${medivault.rabbitmq.queue.ordonnance-delivree}")
    private String queueOrdonnanceDelivree;

    @Value("${medivault.rabbitmq.routing-key.ordonnance-delivree}")
    private String routingKeyOrdonnanceDelivree;

    // Exchange commun à tous les microservices MediVault.
    // Type "topic" : il distribue les messages selon leur clé de routage.
    // true = durable (survit à un redémarrage de RabbitMQ), false = pas supprimé automatiquement.
    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

    // File durable : si notre service est arrêté, les messages attendent dedans (aucun perdu)
    @Bean
    public Queue ordonnanceDelivreeQueue() {
        return QueueBuilder.durable(queueOrdonnanceDelivree).build();
    }

    // Binding : relie notre file à l'exchange pour la clé "ordonnance.delivree"
    @Bean
    public Binding ordonnanceDelivreeBinding(Queue ordonnanceDelivreeQueue, TopicExchange medivaultExchange) {
        return BindingBuilder.bind(ordonnanceDelivreeQueue).to(medivaultExchange).with(routingKeyOrdonnanceDelivree);
    }

    // Messages en JSON (au lieu du format binaire Java) : lisible par tous les services.
    // Le type est déduit du paramètre du listener
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
