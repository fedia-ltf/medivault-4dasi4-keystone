package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultatDisponible(
        String analyseId,
        String patientId,
        String medecinId,
        String laboratoireId,
        String typeAnalyse,
        LocalDateTime date
) {}
