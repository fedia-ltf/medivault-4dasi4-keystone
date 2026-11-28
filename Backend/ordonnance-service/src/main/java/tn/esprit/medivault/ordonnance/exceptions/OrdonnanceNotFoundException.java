package tn.esprit.medivault.ordonnance.exceptions;

public class OrdonnanceNotFoundException extends RuntimeException {
    public OrdonnanceNotFoundException(String code) {
        super("Ordonnance introuvable : " + code);
    }
}