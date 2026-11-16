package tn.esprit.medivault.dossier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * dossier-service : gestion des dossiers médicaux, consultations et accès aux dossiers.
 * Base MySQL (dossier_db).
 */
@SpringBootApplication
@EnableFeignClients
public class DossierServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DossierServiceApplication.class, args);
    }
}
