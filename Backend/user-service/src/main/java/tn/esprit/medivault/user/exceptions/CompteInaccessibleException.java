package tn.esprit.medivault.user.exceptions;

/** Le compte existe mais ne peut pas se connecter : désactivé, ou sans profil MediVault (403). */
public class CompteInaccessibleException extends RuntimeException {
    public CompteInaccessibleException(String message) {
        super(message);
    }
}
