package tn.esprit.medivault.ordonnance.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneOrdonnanceResponseDTO {
    private Long id;
    private Long medicamentId;
    private String nomMedicament;
    private String posologie;
    private int dureeJours;
    private int quantite;
    private int quantiteDelivree;
}