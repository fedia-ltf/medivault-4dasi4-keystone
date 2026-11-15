package tn.esprit.medivault.medicament.exceptions;

// Exception "maison" lancée quand un id de médicament n'existe pas.
// Elle hérite de RuntimeException : pas besoin de "throws" partout.
// Le GlobalExceptionHandler l'attrape et la transforme en réponse 404.
public class MedicamentNotFoundException extends RuntimeException {

    public MedicamentNotFoundException(Long id) {
        super("Médicament introuvable avec l'id " + id);
    }
}
