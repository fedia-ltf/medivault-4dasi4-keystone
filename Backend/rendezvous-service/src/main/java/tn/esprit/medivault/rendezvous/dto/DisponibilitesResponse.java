package tn.esprit.medivault.rendezvous.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Réponse renvoyée par user-service sur GET /api/professionnels/{id}/disponibilites.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilitesResponse {
    private Long id;
    private String typeProfessionnel;
    private List<String> joursTravail;
    private LocalTime heureDebut;
    private LocalTime heureFin;
}

