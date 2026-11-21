package tn.esprit.medivault.medicament.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tn.esprit.medivault.medicament.clients.OrdonnanceClient;
import tn.esprit.medivault.medicament.clients.UtilisateurClient;
import tn.esprit.medivault.medicament.dto.ExterneDtos;
import tn.esprit.medivault.medicament.exceptions.ObjetExterneIntrouvableException;

import java.util.List;

// Mode "reel" : vraies communications synchrones (Feign + Eureka + Resilience4j).
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "medivault.integration.mode", havingValue = "reel")
public class VerificationFeign implements VerificationExterne {

    private final OrdonnanceClient ordonnances;
    private final UtilisateurClient utilisateurs;

    @Override
    public OrdonnanceVerifiee ordonnance(String code) {
        try {
            ExterneDtos.Ordonnance o = ordonnances.parCode(code);
            if (o == null) {
                return null;
            }
            List<OrdonnanceVerifiee.Ligne> lignes = o.lignes() == null ? List.of()
                    : o.lignes().stream().map(l -> new OrdonnanceVerifiee.Ligne(l.medicamentId(), l.quantite())).toList();
            return new OrdonnanceVerifiee(o.id(), o.code(), o.patientId(), o.statut(), lignes);
        } catch (ObjetExterneIntrouvableException e) {
            return null;
        }
    }

    @Override
    public boolean pharmacieActive(String pharmacieId) {
        try {
            ExterneDtos.Utilisateur u = utilisateurs.parId(pharmacieId);
            return u != null && u.actif();
        } catch (ObjetExterneIntrouvableException e) {
            return false;
        }
    }
}