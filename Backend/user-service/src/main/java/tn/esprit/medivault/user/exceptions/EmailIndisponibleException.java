package tn.esprit.medivault.user.exceptions;

/** Le service d'e-mail (API) n'a pas accepté le message. Jamais montré à l'appelant de « mot de passe oublié » : seulement tracé. */
public class EmailIndisponibleException extends RuntimeException {
    public EmailIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
