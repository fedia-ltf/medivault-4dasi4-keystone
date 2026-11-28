package tn.esprit.medivault.medicament.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Mode "simule" (par défaut) : permet de tester toute l'API sans ordonnance-service ni user-service.
// Ordonnances fictives (id des médicaments = ceux des données de démonstration) :
//   ORD-A7F3 et ORD-B219 : VALIDEE, tous les médicaments, 20 boîtes chacun
//   ORD-C884             : VALIDEE, seulement les médicaments 1 et 3, 10 boîtes chacun
//   ORD-D102             : EN_ATTENTE_VERIFICATION (refusée par RG1)
//   ORD-E410             : DELIVREE (refusée par RG1)
//   toute autre ordonnance : introuvable
// Pharmacie inactive : tout identifiant qui commence par "off-".
@Service
@ConditionalOnProperty(name = "medivault.integration.mode", havingValue = "simule", matchIfMissing = true)
public class VerificationSimulee implements VerificationExterne {

    private static final List<OrdonnanceVerifiee.Ligne> TOUS = List.of(
            ligne(1L, 20), ligne(2L, 20), ligne(3L, 20), ligne(4L, 20), ligne(5L, 20), ligne(6L, 20), ligne(7L, 20));

    private static final Map<String, OrdonnanceVerifiee> ORDONNANCES = Map.of(
            "ORD-A7F3", new OrdonnanceVerifiee(101L, "ORD-A7F3", "patient-1", "VALIDEE", TOUS),
            "ORD-B219", new OrdonnanceVerifiee(102L, "ORD-B219", "patient-2", "VALIDEE", TOUS),
            "ORD-C884", new OrdonnanceVerifiee(103L, "ORD-C884", "patient-3", "VALIDEE", List.of(ligne(1L, 10), ligne(3L, 10))),
            "ORD-D102", new OrdonnanceVerifiee(104L, "ORD-D102", "patient-4", "EN_ATTENTE_VERIFICATION", TOUS),
            "ORD-E410", new OrdonnanceVerifiee(105L, "ORD-E410", "patient-5", "DELIVREE", TOUS));

    private static OrdonnanceVerifiee.Ligne ligne(Long medicamentId, int quantite) {
        return new OrdonnanceVerifiee.Ligne(medicamentId, quantite);
    }

    @Override
    public OrdonnanceVerifiee ordonnance(String code) {
        return ORDONNANCES.get(code.toUpperCase());
    }

    @Override
    public boolean pharmacieActive(String pharmacieId) {
        return !pharmacieId.startsWith("off-");
    }
}