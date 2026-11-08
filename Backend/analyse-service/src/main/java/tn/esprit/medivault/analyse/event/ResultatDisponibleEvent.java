package tn.esprit.medivault.analyse.event;

/** Publié (clé resultat.disponible) vers dossier-service et user-service. */
public record ResultatDisponibleEvent(Long analyseId, Long patientId, Long laboratoireId, String typeAnalyse) {
}
