package tn.esprit.medivault.ordonnance.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement publié par medicament-service (clé vente.sensible.enregistree) après une vente
 * de médicament sensible sur ordonnance. Mêmes champs que VenteSensibleEnregistree côté medicament.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VenteSensibleEnregistreeEvent {
    private Long ordonnanceId;
    private Long medicamentId;
    private int quantite;
    private String pharmacieId;
    private LocalDateTime date;
}
