package tn.esprit.medivault.analyse.exception;

/** RG10 ou justification d'accès retirée / expirée (403). */
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException(String message) {
        super(message);
    }
}
