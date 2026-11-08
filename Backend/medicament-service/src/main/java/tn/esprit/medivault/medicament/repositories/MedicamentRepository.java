package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.medicament.entities.Medicament;

@Repository
public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
}
