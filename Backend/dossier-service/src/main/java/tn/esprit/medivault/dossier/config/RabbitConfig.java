package tn.esprit.medivault.dossier.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "medivault.events";

    public static final String QUEUE_UTILISATEUR_CREE = "dossier.utilisateur-cree";
    public static final String ROUTING_KEY_UTILISATEUR_CREE = "utilisateur.cree";

    public static final String QUEUE_RENDEZVOUS_CONFIRME = "dossier.rendezvous-confirme";
    public static final String ROUTING_KEY_RENDEZVOUS_CONFIRME = "rendezvous.confirme";

    public static final String QUEUE_RENDEZVOUS_ANNULE = "dossier.rendezvous-annule";
    public static final String ROUTING_KEY_RENDEZVOUS_ANNULE = "rendezvous.annule";

    public static final String QUEUE_ORDONNANCE_CREEE = "dossier.ordonnance-creee";
    public static final String ROUTING_KEY_ORDONNANCE_CREEE = "ordonnance.creee";

    public static final String QUEUE_RESULTAT_DISPONIBLE = "dossier.resultat-disponible";
    public static final String ROUTING_KEY_RESULTAT_DISPONIBLE = "resultat.disponible";

    public static final String QUEUE_SOIN_REALISE = "dossier.soin-realise";
    public static final String ROUTING_KEY_SOIN_REALISE = "soin.realise";

    @Bean
    public TopicExchange medivaultEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue utilisateurCreeQueue() {
        return QueueBuilder.durable(QUEUE_UTILISATEUR_CREE).build();
    }

    @Bean
    public Binding utilisateurCreeBinding() {
        return BindingBuilder.bind(utilisateurCreeQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_UTILISATEUR_CREE);
    }

    @Bean
    public Queue rendezVousConfirmeQueue() {
        return QueueBuilder.durable(QUEUE_RENDEZVOUS_CONFIRME).build();
    }

    @Bean
    public Binding rendezVousConfirmeBinding() {
        return BindingBuilder.bind(rendezVousConfirmeQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_RENDEZVOUS_CONFIRME);
    }

    @Bean
    public Queue rendezVousAnnuleQueue() {
        return QueueBuilder.durable(QUEUE_RENDEZVOUS_ANNULE).build();
    }

    @Bean
    public Binding rendezVousAnnuleBinding() {
        return BindingBuilder.bind(rendezVousAnnuleQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_RENDEZVOUS_ANNULE);
    }

    @Bean
    public Queue ordonnanceCreeeQueue() {
        return QueueBuilder.durable(QUEUE_ORDONNANCE_CREEE).build();
    }

    @Bean
    public Binding ordonnanceCreeeBinding() {
        return BindingBuilder.bind(ordonnanceCreeeQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_ORDONNANCE_CREEE);
    }

    @Bean
    public Queue resultatDisponibleQueue() {
        return QueueBuilder.durable(QUEUE_RESULTAT_DISPONIBLE).build();
    }

    @Bean
    public Binding resultatDisponibleBinding() {
        return BindingBuilder.bind(resultatDisponibleQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_RESULTAT_DISPONIBLE);
    }

    @Bean
    public Queue soinRealiseQueue() {
        return QueueBuilder.durable(QUEUE_SOIN_REALISE).build();
    }

    @Bean
    public Binding soinRealiseBinding() {
        return BindingBuilder.bind(soinRealiseQueue()).to(medivaultEventsExchange()).with(ROUTING_KEY_SOIN_REALISE);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
