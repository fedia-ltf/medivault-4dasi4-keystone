package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RendezVousConfirme(
        String rendezVousId,
        String patientId,
        String professionnelId,
        String typeProfessionnel,
        String analyseId,
        LocalDateTime date
) {}
