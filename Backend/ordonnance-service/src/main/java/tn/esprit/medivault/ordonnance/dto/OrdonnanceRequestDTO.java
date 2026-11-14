package tn.esprit.medivault.ordonnance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdonnanceRequestDTO {
    private Long patientId;
    private Long medecinId;
    private Long consultationId;
    private List<LigneOrdonnanceDTO> lignes;
}