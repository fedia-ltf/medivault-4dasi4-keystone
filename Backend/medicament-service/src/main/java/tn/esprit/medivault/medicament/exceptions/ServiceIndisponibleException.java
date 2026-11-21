package tn.esprit.medivault.medicament.exceptions;

// Un service dont on dépend (ordonnance ou user) ne répond pas  ->  503
// Règle de la documentation : "pas de vérification, pas de vente".
public class ServiceIndisponibleException extends RuntimeException {

    public ServiceIndisponibleException(String service, Throwable cause) {
        super(service + " est indisponible : la vente est refusée tant qu'il ne répond pas.", cause);
    }
}