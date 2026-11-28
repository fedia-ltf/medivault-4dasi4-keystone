package tn.esprit.medivault.soindomicile.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.soindomicile.clients.OrdonnanceClient;
import tn.esprit.medivault.soindomicile.dto.OrdonnanceDTO;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import tn.esprit.medivault.soindomicile.exceptions.OrdonnanceInvalideException;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VerificationOrdonnance {

    private final OrdonnanceClient ordonnanceClient;

    @Value("${medivault.ordonnance.statuts-acceptes}")
    private List<String> statutsAcceptes;

    public void verifier(TypeSoin typeSoin, Long ordonnanceId, String patientId) {
        if (!typeSoin.necessiteOrdonnance()) return;          // tension, glycémie : pas d'ordonnance
        if (ordonnanceId == null) {
            throw new OrdonnanceInvalideException("Ordonnance obligatoire pour " + typeSoin);
        }
        OrdonnanceDTO o = ordonnanceClient.getById(ordonnanceId);  // appel synchrone : on attend la réponse
        if (!patientId.equals(o.patientId())) {
            throw new OrdonnanceInvalideException("Cette ordonnance n'appartient pas au patient");
        }
        if (!statutsAcceptes.contains(o.statut())) {
            throw new OrdonnanceInvalideException("Ordonnance non valide (statut " + o.statut() + ")");
        }
        if (o.dateExpiration() != null && o.dateExpiration().isBefore(LocalDate.now())) {
            throw new OrdonnanceInvalideException("Ordonnance expirée");
        }
    }
}
