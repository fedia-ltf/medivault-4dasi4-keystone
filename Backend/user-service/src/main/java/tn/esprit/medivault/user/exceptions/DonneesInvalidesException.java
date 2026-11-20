package tn.esprit.medivault.user.exceptions;

/** Règle métier non respectée sur les données envoyées (400). */
public class DonneesInvalidesException extends RuntimeException {
    public DonneesInvalidesException(String message) {
        super(message);
    }
}
