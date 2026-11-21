package tn.esprit.medivault.rendezvous.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consommé depuis RabbitMQ (clé consultation.ajoutee) :
 * Clôture le rendez-vous lié à la consultation et au patient (statut TERMINE).
 */
public record ConsultationAjouteeEvent(
        Long consultationId,
        String patientId,
        String medecinId,
        List<String> analysesDemandees,
        LocalDateTime date
) {
}
