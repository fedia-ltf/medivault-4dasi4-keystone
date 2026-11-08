package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Événement publié sur RabbitMQ lors du retrait d'un médicament (RG6)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentRetireEvent {

    private Long medicamentId;
    private String motif;
    private LocalDateTime date;
}
