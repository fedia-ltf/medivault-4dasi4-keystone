package tn.esprit.medivault.rendezvous.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;
import tn.esprit.medivault.rendezvous.enums.TypeProfessionnel;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVousResponse {
    private Long id;
    private Long patientId;
    private String motif;
    private StatutRdv statut;
    private Long creneauId;
    private Long professionnelId;
    private TypeProfessionnel typeProfessionnel;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
}

