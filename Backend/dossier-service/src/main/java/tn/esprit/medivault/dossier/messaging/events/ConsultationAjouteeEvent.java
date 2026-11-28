package tn.esprit.medivault.dossier.messaging.events;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultationAjouteeEvent(
        Long consultationId,
        String patientId,
        String medecinId,
        List<String> analysesDemandees,
        LocalDateTime date
) {}
