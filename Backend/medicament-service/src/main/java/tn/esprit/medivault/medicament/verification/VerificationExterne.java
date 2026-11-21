package tn.esprit.medivault.medicament.verification;

// Les deux vérifications que medicament-service demande aux autres microservices avant une vente.
// Deux implémentations existent (voir medivault.integration.mode dans application.properties) :
//   - "reel"   : VerificationFeign, appelle vraiment ordonnance-service et user-service
//   - "simule" : VerificationSimulee, des données fictives pour travailler seul (par défaut)
public interface VerificationExterne {

    // L'ordonnance trouvée, ou null si elle n'existe pas.
    // Lance ServiceIndisponibleException (503) si ordonnance-service ne répond pas.
    OrdonnanceVerifiee ordonnance(String code);

    // true si le compte du pharmacien est actif. Lance ServiceIndisponibleException (503) si user-service ne répond pas.
    boolean pharmacieActive(String pharmacieId);
}