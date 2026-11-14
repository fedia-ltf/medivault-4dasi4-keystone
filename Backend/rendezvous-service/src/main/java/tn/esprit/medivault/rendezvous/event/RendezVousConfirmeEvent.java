package tn.esprit.medivault.rendezvous.event;

import java.time.LocalDateTime;

/**
 * Publié sur RabbitMQ (exchange medivault.events, clé rendezvous.confirme) :
 * - dossier-service : crée automatiquement AccesDossier ACTIF pour le praticien
 * - analyse-service : cette analyse passe EN_COURS + affectation laboratoireId
 * - notification-service : notification SSE vers le patient et le professionnel
 */
public record RendezVousConfirmeEvent(
        Long rendezVousId,
        String patientId,
        String professionnelId,
        String typeProfessionnel,
        Long analyseId,
        LocalDateTime date
) {
}
