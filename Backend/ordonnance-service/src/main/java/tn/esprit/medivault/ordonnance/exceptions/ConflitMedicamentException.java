package tn.esprit.medivault.ordonnance.exceptions;

import lombok.Getter;
import java.util.List;

@Getter
public class ConflitMedicamentException extends RuntimeException {
    private final List<String> conflits;

    public ConflitMedicamentException(List<String> conflits) {
        super("Conflit médicament détecté");
        this.conflits = conflits;
    }
}