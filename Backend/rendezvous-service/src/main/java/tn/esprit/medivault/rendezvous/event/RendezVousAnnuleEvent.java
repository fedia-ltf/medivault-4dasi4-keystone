package tn.esprit.medivault.rendezvous.event;

/**
 * Publié sur RabbitMQ (exchange medivault.events, clé rendezvous.annule) lors de l'annulation d'un RDV :
 * - dossier-service : l'accès lié passe à RETIRE
 * - analyse-service : si labo, l'analyse repasse à EN_ATTENTE et le laboratoireId est vidé
 */
public record RendezVousAnnuleEvent(
        Long rendezVousId,
        String patientId,
        String professionnelId,
        String typeProfessionnel,
        Long analyseId
) {
}

