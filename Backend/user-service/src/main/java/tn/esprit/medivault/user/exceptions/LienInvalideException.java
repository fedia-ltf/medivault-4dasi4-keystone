package tn.esprit.medivault.user.exceptions;

/** Le lien de réinitialisation du mot de passe est inconnu, déjà utilisé ou expiré (400). */
public class LienInvalideException extends RuntimeException {
    public LienInvalideException(String message) {
        super(message);
    }
}
