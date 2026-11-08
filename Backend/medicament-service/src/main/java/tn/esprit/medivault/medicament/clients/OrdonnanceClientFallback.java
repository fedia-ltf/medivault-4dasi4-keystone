package tn.esprit.medivault.medicament.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.OrdonnanceDTO;

import java.util.List;

// Utilisé quand ordonnance-service est indisponible : le retrait continue sans bloquer
@Component
@Slf4j
public class OrdonnanceClientFallback implements OrdonnanceClient {

    @Override
    public List<OrdonnanceDTO> getOrdonnances(Long medicamentId, String statut) {
        log.warn("ordonnance-service indisponible : impossible de vérifier les ordonnances du médicament {}", medicamentId);
        return List.of();
    }
}
