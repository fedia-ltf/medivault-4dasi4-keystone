package tn.esprit.medivault.soindomicile.dto;

import java.time.LocalDateTime;

public record SoinRealiseEvent(Long visiteId, String patientId, String pharmacieId, String pharmacienId,
                               String infirmierNom,
                               String typeSoin, LocalDateTime date, String tension, Double glycemie,
                               boolean injectionFaite, String dose, String observations) {
}
