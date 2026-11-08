package tn.esprit.medivault.analyse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * analyse-service : analyses biologiques demandées en consultation et saisie des résultats par le laboratoire.
 * Base H2, enregistré dans Eureka sous le nom ANALYSE-SERVICE, exposé via la Gateway sur /api/analyses/**.
 */
@SpringBootApplication
@EnableFeignClients
public class AnalyseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyseServiceApplication.class, args);
    }
}
