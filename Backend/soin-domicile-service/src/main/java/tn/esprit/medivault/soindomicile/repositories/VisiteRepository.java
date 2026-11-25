package tn.esprit.medivault.soindomicile.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import tn.esprit.medivault.soindomicile.entities.Visite;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VisiteRepository extends JpaRepository<Visite, Long>, JpaSpecificationExecutor<Visite> {

    List<Visite> findByPatientIdOrderByDateAsc(String patientId);

    List<Visite> findByPharmacieIdOrderByDateAsc(String pharmacieId);

    List<Visite> findByPharmacieIdAndStatutOrderByDateAsc(String pharmacieId, StatutVisite statut);

    List<Visite> findByPlan_IdOrderByDateAsc(Long planId);

    /** RG3 : le patient a-t-il déjà une visite active à cette heure ? */
    boolean existsByPatientIdAndDateAndStatutIn(String patientId, LocalDateTime date, Collection<StatutVisite> statuts);

    /** RG8 : injections prévues avec un médicament retiré. */
    List<Visite> findByMedicamentIdAndTypeSoinAndStatutIn(Long medicamentId, TypeSoin typeSoin, Collection<StatutVisite> statuts);

    /** RG9 : l'infirmier a-t-il déjà un soin confirmé dans ce créneau ? */
    boolean existsByPharmacieIdAndInfirmierNomAndStatutAndDateBetween(String pharmacieId, String infirmierNom,
                                                                     StatutVisite statut, LocalDateTime debut, LocalDateTime fin);

    /** Soins confirmés d'un infirmier (mis à jour quand le pharmacien le renomme). */
    List<Visite> findByPharmacieIdAndInfirmierNomAndStatut(String pharmacieId, String infirmierNom, StatutVisite statut);

    /** L'infirmier a-t-il encore des soins confirmés à venir ? (avant de le supprimer) */
    boolean existsByPharmacieIdAndInfirmierNomAndStatutAndDateAfter(String pharmacieId, String infirmierNom,
                                                                  StatutVisite statut, LocalDateTime date);

    /** Visites futures d'un plan, à annuler quand le plan change ou s'arrête. */
    List<Visite> findByPlan_IdAndDateAfterAndStatutIn(Long planId, LocalDateTime date, Collection<StatutVisite> statuts);
}
