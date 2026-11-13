package tn.esprit.medivault.rendezvous.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.rendezvous.enums.TypeProfessionnel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreneauResponse {
    private Long id;
    private Long professionnelId;
    private TypeProfessionnel typeProfessionnel;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean libre;
}

