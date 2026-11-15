package tn.esprit.medivault.medicament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Point de départ du microservice : c'est cette classe qu'on lance avec ▶ dans IntelliJ.
// @SpringBootApplication : active Spring Boot (scanne tous les packages en dessous,
//   crée les objets @Service, @Controller, @Repository... et démarre Tomcat sur le port 8084).
// @EnableFeignClients : active OpenFeign, pour que l'interface OrdonnanceClient
//   devienne un vrai client HTTP vers ordonnance-service.
@SpringBootApplication
@EnableFeignClients
public class MedicamentServiceApplication {

	public static void main(String[] args) {
		// Démarre toute l'application (serveur web, base H2, inscription dans Eureka, RabbitMQ...)
		SpringApplication.run(MedicamentServiceApplication.class, args);
	}

}
