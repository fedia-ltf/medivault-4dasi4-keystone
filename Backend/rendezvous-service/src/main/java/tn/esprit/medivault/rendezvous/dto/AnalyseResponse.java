package tn.esprit.medivault.rendezvous.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse renvoyée par analyse-service sur GET /api/analyses/{id}.
 * Utilisé pour vérifier la prescription avant réservation au laboratoire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyseResponse {
    private Long id;
    private String patientId;
    private String medecinId;
    private String laboratoireId;
    private String typeAnalyse;
    private String statut;
}

