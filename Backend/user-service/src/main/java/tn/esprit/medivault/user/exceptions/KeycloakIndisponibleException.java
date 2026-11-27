package tn.esprit.medivault.user.exceptions;

/** Keycloak injoignable : aucun compte n'est créé sans son identité Keycloak (503). */
public class KeycloakIndisponibleException extends RuntimeException {
    public KeycloakIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
