package tn.esprit.medivault.dossier.exceptions;

public class DossierNotFoundException extends RuntimeException {

    public DossierNotFoundException(String message) {
        super(message);
    }
}
