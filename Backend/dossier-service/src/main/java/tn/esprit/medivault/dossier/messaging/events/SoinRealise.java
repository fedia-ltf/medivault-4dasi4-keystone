package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoinRealise(
        String visiteId,
        String patientId,
        String typeSoin,
        String tension,
        String glycemie,
        String observations,
        @JsonAlias("date") LocalDateTime dateRapport
) {}
