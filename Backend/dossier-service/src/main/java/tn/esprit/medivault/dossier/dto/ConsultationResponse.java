package tn.esprit.medivault.dossier.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultationResponse(
        Long id,
        String medecinId,
        LocalDateTime date,
        String motif,
        String diagnostic,
        String observations,
        List<String> analysesDemandees
) {}
