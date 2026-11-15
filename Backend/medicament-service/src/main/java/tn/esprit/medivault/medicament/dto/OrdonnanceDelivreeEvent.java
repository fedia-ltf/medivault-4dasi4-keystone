package tn.esprit.medivault.medicament.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Événement REÇU de RabbitMQ (publié par pharmacie-service) quand une ordonnance est délivrée.
// On s'en sert pour augmenter nb_delivrances de chaque médicament délivré.
// ignoreUnknown = true : on ignore les champs qu'on ne connaît pas.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdonnanceDelivreeEvent {

    private Long ordonnanceId;
    private Long patientId;
    // La liste des médicaments délivrés (une ligne = un médicament + sa quantité)
    private List<LigneDelivree> lignesDelivrees = new ArrayList<>();
    // TOTALE ou PARTIELLE
    private String type;

    // Classe interne : une ligne de la délivrance
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LigneDelivree {
        private Long medicamentId;
        private Integer quantite;
    }
}
