package tn.esprit.medivault.ordonnance.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompatibiliteRequestDTO {
    private Long patientId;
    private List<Long> medicamentIds;
    private List<String> allergies;
}