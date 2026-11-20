package tn.esprit.medivault.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * user-service publie UtilisateurCree et ne consomme aucun événement
 * (les notifications sont gérées par notification-service) : pas de file à déclarer.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${medivault.rabbitmq.exchange}")
    private String exchange;

    // Exchange commun à tous les microservices MediVault
    @Bean
    public TopicExchange medivaultExchange() {
        return new TopicExchange(exchange, true, false);
    }

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
