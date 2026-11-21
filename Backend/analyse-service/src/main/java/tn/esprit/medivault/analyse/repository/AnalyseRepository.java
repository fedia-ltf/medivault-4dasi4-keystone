package tn.esprit.medivault.analyse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import java.util.List;

public interface AnalyseRepository extends JpaRepository<Analyse, Long> {

    /** GET /api/analyses?statut=EN_COURS&laboratoireId= */
    List<Analyse> findByStatutAndLaboratoireIdOrderByDateDemandeAsc(StatutAnalyse statut, String laboratoireId);

    List<Analyse> findByStatutOrderByDateDemandeAsc(StatutAnalyse statut);

    List<Analyse> findByLaboratoireIdOrderByDateDemandeAsc(String laboratoireId);

    /** GET /api/analyses/patient/{patientId} */
    List<Analyse> findByPatientIdOrderByDateDemandeDesc(String patientId);

    boolean existsByConsultationIdAndTypeAnalyse(Long consultationId, String typeAnalyse);
}
