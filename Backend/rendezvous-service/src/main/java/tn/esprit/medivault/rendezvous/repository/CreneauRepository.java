package tn.esprit.medivault.rendezvous.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.rendezvous.entity.Creneau;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    List<Creneau> findByProfessionnelId(Long professionnelId);

    @Query("SELECT c FROM Creneau c WHERE c.professionnelId = :professionnelId " +
           "AND c.dateDebut >= :debutJournee AND c.dateDebut < :finJournee")
    List<Creneau> findByProfessionnelIdAndDate(
            @Param("professionnelId") Long professionnelId,
            @Param("debutJournee") LocalDateTime debutJournee,
            @Param("finJournee") LocalDateTime finJournee
    );
}

