package tn.esprit.medivault.medicament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MedicamentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicamentServiceApplication.class, args);
	}

}
