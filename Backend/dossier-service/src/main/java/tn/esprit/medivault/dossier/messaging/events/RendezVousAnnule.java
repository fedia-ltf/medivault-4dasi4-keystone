package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RendezVousAnnule(
        String rendezVousId,
        String patientId,
        String professionnelId,
        String typeProfessionnel,
        String analyseId
) {}
