package tn.esprit.medivault.rendezvous.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVousRequest {

    @NotNull(message = "L'identifiant du patient est obligatoire")
    private Long patientId;

    @NotNull(message = "L'identifiant du créneau est obligatoire")
    private Long creneauId;

    @NotBlank(message = "Le motif du rendez-vous est obligatoire")
    private String motif;
}

