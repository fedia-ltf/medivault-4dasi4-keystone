package tn.esprit.medivault.ordonnance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdonnanceResponseDTO {
    private Long id;
    private String code;
    private Long patientId;
    private Long medecinId;
    private String dateEmission;
    private String dateExpiration;
    private String statut;
    private List<LigneOrdonnanceResponseDTO> lignes;
}