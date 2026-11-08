package tn.esprit.medivault.medicament.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Événement reçu de pharmacie/ordonnance-service quand une ordonnance est délivrée
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdonnanceDelivreeEvent {

    private Long ordonnanceId;
    private Long patientId;
    private List<LigneDelivree> lignesDelivrees = new ArrayList<>();
    // TOTALE ou PARTIELLE
    private String type;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LigneDelivree {
        private Long medicamentId;
        private Integer quantite;
    }
}
