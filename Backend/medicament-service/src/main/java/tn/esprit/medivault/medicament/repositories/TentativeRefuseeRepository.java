package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.medivault.medicament.entities.TentativeRefusee;

import java.time.LocalDateTime;
import java.util.List;

public interface TentativeRefuseeRepository extends JpaRepository<TentativeRefusee, Long>, JpaSpecificationExecutor<TentativeRefusee> {

    // Les refus d'une période [debut, fin[ : sert au score de risque d'un mois
    List<TentativeRefusee> findByDateGreaterThanEqualAndDateLessThan(LocalDateTime debut, LocalDateTime fin);
}
