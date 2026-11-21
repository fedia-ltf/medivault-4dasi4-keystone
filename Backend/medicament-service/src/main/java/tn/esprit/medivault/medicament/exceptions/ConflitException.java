package tn.esprit.medivault.medicament.exceptions;

// L'opération est impossible à cause de l'état actuel des données (doublon, quota déjà vendu...)  ->  409
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}