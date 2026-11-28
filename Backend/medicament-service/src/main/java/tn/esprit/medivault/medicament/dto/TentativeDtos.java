package tn.esprit.medivault.medicament.dto;

import tn.esprit.medivault.medicament.entities.MotifRefus;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;

import java.time.LocalDateTime;

public final class TentativeDtos {

    private TentativeDtos() {
    }

    public record Reponse(Long id, String pharmacieId, Long medicamentId, String medicamentNom, String ordonnanceCode,
                          int quantite, LocalDateTime date, MotifRefus motif, String detail) {
        public static Reponse de(TentativeRefusee t) {
            return new Reponse(t.getId(), t.getPharmacieId(), t.getMedicamentId(), t.getMedicamentNom(),
                    t.getOrdonnanceCode(), t.getQuantite(), t.getDate(), t.getMotif(), t.getDetail());
        }
    }
}
