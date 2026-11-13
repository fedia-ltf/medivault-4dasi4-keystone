package tn.esprit.medivault.rendezvous;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * rendezvous-service : gestion des créneaux de consultation/analyses et prise de rendez-vous.
 * Base H2, enregistré dans Eureka sous le nom RENDEZVOUS-SERVICE, exposé via la Gateway sur /api/creneaux/** et /api/rendezvous/**.
 */
@SpringBootApplication
@EnableFeignClients
public class RendezvousServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RendezvousServiceApplication.class, args);
    }
}

