package tn.esprit.medivault.ordonnance.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdonnanceDelivreeEvent {
    private Long ordonnanceId;
    private Long patientId;
    private List<LigneDelivree> lignesDelivrees;
    private String type;

    // Inner class for the lines
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneDelivree {
        private Long medicamentId;
        private int quantite;
    }
}