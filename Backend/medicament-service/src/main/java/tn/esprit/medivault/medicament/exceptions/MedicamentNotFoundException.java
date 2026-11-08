package tn.esprit.medivault.medicament.exceptions;

public class MedicamentNotFoundException extends RuntimeException {

    public MedicamentNotFoundException(Long id) {
        super("Médicament introuvable avec l'id " + id);
    }
}
