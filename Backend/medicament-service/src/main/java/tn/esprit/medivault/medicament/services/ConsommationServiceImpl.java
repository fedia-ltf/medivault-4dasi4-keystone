package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;
import tn.esprit.medivault.medicament.repositories.MedicamentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsommationServiceImpl implements IConsommationService {

    private final MedicamentRepository medicamentRepository;

    @Override
    @Transactional
    public void enregistrerDelivrance(OrdonnanceDelivreeEvent event) {
        if (event.getLignesDelivrees() == null) {
            return;
        }
        for (OrdonnanceDelivreeEvent.LigneDelivree ligne : event.getLignesDelivrees()) {
            if (ligne.getMedicamentId() == null) {
                continue;
            }
            // Un médicament retiré entre-temps n'est plus dans le catalogue : on l'ignore
            medicamentRepository.findById(ligne.getMedicamentId()).ifPresentOrElse(
                    medicament -> medicament.enregistrerDelivrance(),
                    () -> log.warn("Médicament {} introuvable, statistique ignorée", ligne.getMedicamentId()));
        }
        log.info("Ordonnance {} délivrée ({}) : statistiques mises à jour", event.getOrdonnanceId(), event.getType());
    }
}
