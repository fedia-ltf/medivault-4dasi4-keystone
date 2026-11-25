package tn.esprit.medivault.soindomicile.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.medivault.soindomicile.entities.PlanSuivi;

import java.util.List;

public interface PlanSuiviRepository extends JpaRepository<PlanSuivi, Long>, JpaSpecificationExecutor<PlanSuivi> {
    List<PlanSuivi> findByPatientId(String patientId);
    List<PlanSuivi> findByMedecinId(String medecinId);
}
