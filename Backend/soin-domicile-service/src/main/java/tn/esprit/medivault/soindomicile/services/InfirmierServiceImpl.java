package tn.esprit.medivault.soindomicile.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.soindomicile.dto.InfirmierRequest;
import tn.esprit.medivault.soindomicile.entities.Infirmier;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.entities.Visite;
import tn.esprit.medivault.soindomicile.exceptions.RegleMetierException;
import tn.esprit.medivault.soindomicile.exceptions.RessourceIntrouvableException;
import tn.esprit.medivault.soindomicile.repositories.InfirmierRepository;
import tn.esprit.medivault.soindomicile.repositories.VisiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InfirmierServiceImpl implements IInfirmierService {

    private final InfirmierRepository repo;
    private final VisiteRepository visiteRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Infirmier> lister(String pharmacieId) {
        return pharmacieId == null ? repo.findAll() : repo.findByPharmacieIdOrderByNomAscPrenomAsc(pharmacieId);
    }

    @Override
    @Transactional(readOnly = true)
    public Infirmier getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Infirmier " + id + " introuvable"));
    }

    @Override
    @Transactional
    public Infirmier ajouter(InfirmierRequest r) {
        verifierHoraires(r);
        if (repo.existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCase(r.pharmacieId(), r.prenom().trim(), r.nom().trim())) {
            throw new RegleMetierException(r.prenom() + " " + r.nom() + " existe déjà dans cette pharmacie");
        }
        return repo.save(Infirmier.builder()
                .prenom(r.prenom().trim()).nom(r.nom().trim()).telephone(r.telephone())
                .heureDebut(r.heureDebut()).heureFin(r.heureFin())
                .pharmacieId(r.pharmacieId())
                .build());
    }

    @Override
    @Transactional
    public Infirmier modifier(Long id, InfirmierRequest r) {
        verifierHoraires(r);
        Infirmier i = getById(id);
        if (repo.existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCaseAndIdNot(
                i.getPharmacieId(), r.prenom().trim(), r.nom().trim(), id)) {
            throw new RegleMetierException(r.prenom() + " " + r.nom() + " existe déjà dans cette pharmacie");
        }
        String ancienNom = i.getNomComplet();
        i.setPrenom(r.prenom().trim());
        i.setNom(r.nom().trim());
        i.setTelephone(r.telephone());
        i.setHeureDebut(r.heureDebut());
        i.setHeureFin(r.heureFin());

        // Les soins déjà confirmés gardent le bon nom et le bon téléphone
        List<Visite> confirmees = visiteRepo.findByPharmacieIdAndInfirmierNomAndStatut(
                i.getPharmacieId(), ancienNom, StatutVisite.ACCEPTEE);
        confirmees.forEach(v -> { v.setInfirmierNom(i.getNomComplet()); v.setInfirmierTelephone(i.getTelephone()); });
        visiteRepo.saveAll(confirmees);

        return repo.save(i);
    }

    /** On ne supprime pas un infirmier qui a encore des soins confirmés à venir. */
    @Override
    @Transactional
    public void supprimer(Long id) {
        Infirmier i = getById(id);
        if (visiteRepo.existsByPharmacieIdAndInfirmierNomAndStatutAndDateAfter(
                i.getPharmacieId(), i.getNomComplet(), StatutVisite.ACCEPTEE, LocalDateTime.now())) {
            throw new RegleMetierException(i.getNomComplet() + " a encore des soins confirmés : réaffectez-les ou annulez-les d'abord");
        }
        repo.delete(i);
    }

    private void verifierHoraires(InfirmierRequest r) {
        if (!r.heureFin().isAfter(r.heureDebut())) {
            throw new RegleMetierException("L'heure de fin de disponibilité doit être après l'heure de début");
        }
    }
}
