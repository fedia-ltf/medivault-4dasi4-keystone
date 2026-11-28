package tn.esprit.medivault.analyse.exception;

/** RG7 : le résultat ne peut être saisi que si l'analyse est EN_COURS (409). */
public class StatutInvalideException extends RuntimeException {
    public StatutInvalideException(String message) {
        super(message);
    }
}
