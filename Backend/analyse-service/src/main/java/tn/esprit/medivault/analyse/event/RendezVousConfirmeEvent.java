package tn.esprit.medivault.analyse.event;

import java.time.LocalDateTime;

/** Consommé (clé rendezvous.confirme) : si typeProfessionnel = LABORATOIRE, l'analyse passe EN_COURS + laboratoireId. */
public record RendezVousConfirmeEvent(Long rendezVousId, Long patientId, Long professionnelId,
                                      String typeProfessionnel, LocalDateTime date) {
}
