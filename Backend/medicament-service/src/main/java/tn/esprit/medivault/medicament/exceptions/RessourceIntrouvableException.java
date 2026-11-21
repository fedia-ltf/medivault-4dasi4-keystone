package tn.esprit.medivault.medicament.exceptions;

// Ce qu'on cherche n'existe pas  ->  404
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}