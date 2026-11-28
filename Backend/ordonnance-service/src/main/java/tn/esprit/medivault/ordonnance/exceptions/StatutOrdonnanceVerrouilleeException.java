package tn.esprit.medivault.ordonnance.exceptions;

public class StatutOrdonnanceVerrouilleeException extends RuntimeException {
    public StatutOrdonnanceVerrouilleeException(String code) {
        super("L’état de l’ordonnance " + code + " a déjà été modifié par le pharmacien.");
    }
}
