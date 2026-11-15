package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Événement publié sur RabbitMQ lors du retrait d'un médicament (RG6).
// C'est le "message" posté dans la boîte aux lettres (exchange medivault.events,
// clé medicament.retire). pharmacie-service le reçoit et bloque la délivrance.
// Il est converti automatiquement en JSON par RabbitMQConfig.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentRetireEvent {

    private Long medicamentId;
    private String motif;
    private LocalDateTime date;
}
