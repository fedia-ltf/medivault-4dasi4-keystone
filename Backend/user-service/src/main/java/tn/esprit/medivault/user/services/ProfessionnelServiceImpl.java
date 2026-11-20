package tn.esprit.medivault.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.medivault.user.dto.DisponibilitesResponse;
import tn.esprit.medivault.user.dto.SignatureResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.exceptions.DonneesInvalidesException;
import tn.esprit.medivault.user.exceptions.UtilisateurNotFoundException;
import tn.esprit.medivault.user.repositories.ProfessionnelSearchRepository;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfessionnelServiceImpl implements IProfessionnelService {

    private static final Set<String> FORMATS_SIGNATURE = Set.of("image/png", "image/jpeg");

    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelSearchRepository professionnelSearchRepository;

    @Override
    public List<UtilisateurResponse> rechercher(TypeProfessionnel type, String ville) {
        return professionnelSearchRepository.rechercher(type, ville).stream()
                .map(UtilisateurResponse::from)
                .toList();
    }

    /** Lu par rendezvous-service pour générer les créneaux (médecin ou laboratoire). */
    @Override
    public DisponibilitesResponse disponibilites(String id) {
        Professionnel p = chargerProfessionnel(id);
        if (p.getType() == TypeProfessionnel.PHARMACIEN) {
            throw new UtilisateurNotFoundException("La pharmacie " + id + " n'a pas d'agenda de rendez-vous");
        }
        List<String> jours = p.getJoursTravail() == null ? List.of()
                : p.getJoursTravail().stream().map(DayOfWeek::name).toList();
        return new DisponibilitesResponse(p.getId(), p.getType().name(), jours, p.getHeureDebut(), p.getHeureFin());
    }

    /** Lu par ordonnance-service pour signer l'ordonnance. */
    @Override
    public SignatureResponse signature(String medecinId) {
        Professionnel medecin = chargerMedecin(medecinId);
        if (medecin.getSignatureImage() == null) {
            throw new UtilisateurNotFoundException("Le médecin " + medecinId + " n'a pas encore enregistré de signature");
        }
        return versSignature(medecin);
    }

    @Override
    public SignatureResponse enregistrerSignature(String medecinId, MultipartFile fichier) {
        Professionnel medecin = chargerMedecin(medecinId);
        if (fichier == null || fichier.isEmpty()) {
            throw new DonneesInvalidesException("Le fichier de signature est vide");
        }
        String format = fichier.getContentType();
        if (format == null || !FORMATS_SIGNATURE.contains(format)) {
            throw new DonneesInvalidesException("La signature doit être une image PNG ou JPEG");
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(fichier.getBytes());
            medecin.setSignatureImage("data:" + format + ";base64," + base64);
        } catch (IOException e) {
            throw new DonneesInvalidesException("Fichier de signature illisible");
        }
        return versSignature(utilisateurRepository.save(medecin));
    }

    private Professionnel chargerProfessionnel(String id) {
        if (utilisateurRepository.findById(id).orElse(null) instanceof Professionnel p) {
            return p;
        }
        throw new UtilisateurNotFoundException("Aucun professionnel avec l'id " + id);
    }

    private Professionnel chargerMedecin(String id) {
        Professionnel p = chargerProfessionnel(id);
        if (p.getType() != TypeProfessionnel.MEDECIN) {
            throw new UtilisateurNotFoundException("Aucun médecin avec l'id " + id);
        }
        return p;
    }

    private SignatureResponse versSignature(Professionnel medecin) {
        return new SignatureResponse(medecin.getId(), "Dr " + medecin.getPrenom() + " " + medecin.getNom(),
                medecin.getSignatureImage());
    }
}
