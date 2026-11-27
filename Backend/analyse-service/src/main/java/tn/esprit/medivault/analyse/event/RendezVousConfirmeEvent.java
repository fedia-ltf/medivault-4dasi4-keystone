package tn.esprit.medivault.analyse.event;

import java.time.LocalDateTime;

/**
 * Consommé (clé rendezvous.confirme) : si typeProfessionnel = LABORANTIN,
 * l'analyse analyseId passe EN_COURS avec laboratoireId = professionnelId.
 */
public record RendezVousConfirmeEvent(Long rendezVousId, String patientId, String professionnelId,
                                      String typeProfessionnel, Long analyseId, LocalDateTime date) {
}
