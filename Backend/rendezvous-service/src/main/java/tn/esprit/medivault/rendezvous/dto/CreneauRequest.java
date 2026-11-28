package tn.esprit.medivault.rendezvous.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreneauRequest {
    private String professionnelId;
    private TypeProfessionnel typeProfessionnel;
    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime dateDebut;
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime dateFin;
}
