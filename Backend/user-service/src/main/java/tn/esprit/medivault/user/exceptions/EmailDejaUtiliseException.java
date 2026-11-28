package tn.esprit.medivault.user.exceptions;

public class EmailDejaUtiliseException extends RuntimeException {
    public EmailDejaUtiliseException(String email) {
        super("Un compte existe déjà avec l'email " + email);
    }
}
