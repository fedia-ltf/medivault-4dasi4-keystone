package tn.esprit.medivault.soindomicile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MedicamentRetireEvent(Long medicamentId, String motif, LocalDateTime date) {
}
