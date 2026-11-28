package tn.esprit.medivault.user.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Contrat lu par rendezvous-service (OpenFeign) sur GET /api/professionnels/{id}/disponibilites.
 * joursTravail : noms des jours en majuscules (MONDAY, TUESDAY…).
 */
public record DisponibilitesResponse(
        String id,
        String typeProfessionnel,
        List<String> joursTravail,
        LocalTime heureDebut,
        LocalTime heureFin
) {
}
