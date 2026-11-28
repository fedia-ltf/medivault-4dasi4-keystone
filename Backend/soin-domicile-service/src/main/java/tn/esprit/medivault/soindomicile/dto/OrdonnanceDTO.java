package tn.esprit.medivault.soindomicile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdonnanceDTO(Long id, String code, String patientId, String statut, LocalDate dateExpiration) {
}
