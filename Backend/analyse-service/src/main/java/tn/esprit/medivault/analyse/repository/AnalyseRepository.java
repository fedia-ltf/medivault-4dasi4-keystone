package tn.esprit.medivault.analyse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import java.util.List;

public interface AnalyseRepository extends JpaRepository<Analyse, Long> {

    /** GET /api/analyses?statut=EN_COURS&laboratoireId= */
    List<Analyse> findByStatutAndLaboratoireId(StatutAnalyse statut, Long laboratoireId);

    /** GET /api/analyses/patient/{patientId} */
    List<Analyse> findByPatientId(Long patientId);
}
