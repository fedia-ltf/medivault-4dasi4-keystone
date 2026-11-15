package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.medicament.entities.Medicament;

// REPOSITORY = la couche qui parle à la base de données.
// En héritant de JpaRepository<Medicament, Long> (entité, type de l'id),
// on reçoit GRATUITEMENT, sans écrire de SQL :
//   save(m)        -> INSERT ou UPDATE
//   findById(id)   -> SELECT ... WHERE id = ?
//   findAll()      -> SELECT * FROM medicament
//   findAllById(l) -> SELECT ... WHERE id IN (...)
//   delete(m) / deleteById(id) -> DELETE
// Spring crée lui-même la classe qui implémente cette interface.
@Repository
public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
}
