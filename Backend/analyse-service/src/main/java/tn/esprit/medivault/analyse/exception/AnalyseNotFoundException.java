package tn.esprit.medivault.analyse.exception;

public class AnalyseNotFoundException extends RuntimeException {
    public AnalyseNotFoundException(Long id) {
        super("Aucune analyse avec l'id " + id);
    }
}
