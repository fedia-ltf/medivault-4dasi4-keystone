package tn.esprit.medivault.medicament.exceptions;

// Un autre service répond "404" (ordonnance ou compte inconnu). Ce n'est PAS une panne :
// la vente est simplement refusée par les règles métier. Utilisée par les fallbacks Feign.
public class ObjetExterneIntrouvableException extends RuntimeException {

    public ObjetExterneIntrouvableException(String message) {
        super(message);
    }
}