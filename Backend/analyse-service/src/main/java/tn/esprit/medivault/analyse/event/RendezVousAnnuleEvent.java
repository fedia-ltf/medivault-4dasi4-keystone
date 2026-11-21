package tn.esprit.medivault.analyse.event;

/**
 * Consommé (clé rendezvous.annule) : si le rendez-vous au laboratoire est annulé,
 * l'analyse repasse EN_ATTENTE et son laboratoireId est vidé.
 */
public record RendezVousAnnuleEvent(Long rendezVousId, String patientId, String professionnelId,
                                    String typeProfessionnel, Long analyseId) {
}
