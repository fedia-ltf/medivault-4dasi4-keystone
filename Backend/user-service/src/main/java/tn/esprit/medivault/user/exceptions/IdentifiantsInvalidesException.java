package tn.esprit.medivault.user.exceptions;

/** Email ou mot de passe faux, session expirée ou personne de connecté (401). */
public class IdentifiantsInvalidesException extends RuntimeException {
    public IdentifiantsInvalidesException(String message) {
        super(message);
    }
}
