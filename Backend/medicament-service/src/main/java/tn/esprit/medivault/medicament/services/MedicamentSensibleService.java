package tn.esprit.medivault.medicament.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.medicament.dto.MedicamentSensibleDtos.Reponse;
import tn.esprit.medivault.medicament.dto.MedicamentSensibleDtos.Requete;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;
import tn.esprit.medivault.medicament.exceptions.ConflitException;
import tn.esprit.medivault.medicament.exceptions.RessourceIntrouvableException;
import tn.esprit.medivault.medicament.repositories.AttributionRepository;
import tn.esprit.medivault.medicament.repositories.MedicamentSensibleRepository;

import java.util.List;

// CRUD de la liste des médicaments sensibles (réservé à l'administrateur).
// @Transactional : tout ce que fait la méthode est validé ensemble, ou annulé en cas d'erreur.
@Service
@RequiredArgsConstructor
public class MedicamentSensibleService {

    private final MedicamentSensibleRepository medicaments;
    private final AttributionRepository attributions;

    // READ (liste) : filtre facultatif par catégorie et par texte (nom ou DCI)
    @Transactional(readOnly = true)
    public List<Reponse> lister(Categorie categorie, String recherche) {
        String texte = recherche == null ? "" : recherche.trim().toLowerCase();
        return medicaments.findAll(Sort.by("nomCommercial")).stream()
                .filter(m -> categorie == null || m.getCategorie() == categorie)
                .filter(m -> texte.isEmpty()
                        || m.getNomCommercial().toLowerCase().contains(texte)
                        || m.getDci().toLowerCase().contains(texte))
                .map(Reponse::de)
                .toList();
    }

    // READ (un seul)
    @Transactional(readOnly = true)
    public Reponse obtenir(Long id) {
        return Reponse.de(trouver(id));
    }

    // CREATE
    @Transactional
    public Reponse creer(Requete requete) {
        String nom = requete.nomCommercial().trim();
        if (medicaments.existsByNomCommercialIgnoreCase(nom)) {
            throw new ConflitException("Ce médicament existe déjà.");
        }
        MedicamentSensible m = MedicamentSensible.builder()
                .nomCommercial(nom)
                .dci(requete.dci().trim())
                .categorie(requete.categorie())
                .description(requete.description())
                .precautions(requete.precautions())
                .build();
        return Reponse.de(medicaments.save(m));
    }

    // UPDATE : on peut garder son propre nom, mais pas prendre celui d'un autre médicament
    @Transactional
    public Reponse modifier(Long id, Requete requete) {
        MedicamentSensible m = trouver(id);
        String nom = requete.nomCommercial().trim();
        if (medicaments.existsByNomCommercialIgnoreCaseAndIdNot(nom, id)) {
            throw new ConflitException("Ce médicament existe déjà.");
        }
        m.setNomCommercial(nom);
        m.setDci(requete.dci().trim());
        m.setCategorie(requete.categorie());
        m.setDescription(requete.description());
        m.setPrecautions(requete.precautions());
        return Reponse.de(m);
    }

    // DELETE : impossible tant que des pharmacies ont un quota pour ce médicament (l'historique des ventes en dépend)
    @Transactional
    public void supprimer(Long id) {
        MedicamentSensible m = trouver(id);
        if (attributions.existsByMedicamentId(id)) {
            throw new ConflitException("Ce médicament est attribué à des pharmacies : supprimez d'abord ses quotas.");
        }
        medicaments.delete(m);
    }

    private MedicamentSensible trouver(Long id) {
        return medicaments.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament sensible introuvable : " + id));
    }
}