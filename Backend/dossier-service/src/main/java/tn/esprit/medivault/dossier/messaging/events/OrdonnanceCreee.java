package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdonnanceCreee(
        String ordonnanceId,
        String code,
        String patientId,
        String medecinId,
        LocalDate dateEmission
) {}
