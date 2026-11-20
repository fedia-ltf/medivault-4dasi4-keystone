package tn.esprit.medivault.analyse.event;

import java.time.LocalDateTime;

/** Publié (clé resultat.disponible) vers dossier-service et notification-service (patient et médecin). */
public record ResultatDisponibleEvent(Long analyseId, String patientId, String medecinId, String laboratoireId,
                                      String typeAnalyse, LocalDateTime date) {
}
