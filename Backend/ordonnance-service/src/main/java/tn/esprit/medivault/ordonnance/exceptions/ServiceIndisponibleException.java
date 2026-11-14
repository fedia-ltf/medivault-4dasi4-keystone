package tn.esprit.medivault.ordonnance.exceptions;

public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message) {
        super(message);
    }
}