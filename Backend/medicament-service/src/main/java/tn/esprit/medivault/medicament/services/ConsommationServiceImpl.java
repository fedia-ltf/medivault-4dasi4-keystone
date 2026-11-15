package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;
import tn.esprit.medivault.medicament.repositories.MedicamentRepository;

// @Service : Spring crée un seul objet de cette classe et l'injecte là où on en a besoin.
// @RequiredArgsConstructor (Lombok) : crée un constructeur avec les champs "final"
//   -> Spring injecte automatiquement medicamentRepository (injection de dépendances).
// @Slf4j (Lombok) : donne l'objet "log" pour écrire dans la console (log.info, log.warn...).
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsommationServiceImpl implements IConsommationService {

    private final MedicamentRepository medicamentRepository;

    // @Transactional : tout ce qui est fait dans la méthode est enregistré d'un coup dans la base.
    // Grâce à la transaction, modifier l'objet (enregistrerDelivrance) suffit :
    // Hibernate fait l'UPDATE tout seul à la fin, pas besoin d'appeler save().
    @Override
    @Transactional
    public void enregistrerDelivrance(OrdonnanceDelivreeEvent event) {
        if (event.getLignesDelivrees() == null) {
            return;
        }
        // Pour chaque médicament délivré dans l'ordonnance...
        for (OrdonnanceDelivreeEvent.LigneDelivree ligne : event.getLignesDelivrees()) {
            if (ligne.getMedicamentId() == null) {
                continue;
            }
            // ...on le cherche dans la base : s'il existe, compteur + 1 ;
            // un médicament retiré entre-temps n'est plus dans le catalogue : on l'ignore
            medicamentRepository.findById(ligne.getMedicamentId()).ifPresentOrElse(
                    medicament -> medicament.enregistrerDelivrance(),
                    () -> log.warn("Médicament {} introuvable, statistique ignorée", ligne.getMedicamentId()));
        }
        log.info("Ordonnance {} délivrée ({}) : statistiques mises à jour", event.getOrdonnanceId(), event.getType());
    }
}
