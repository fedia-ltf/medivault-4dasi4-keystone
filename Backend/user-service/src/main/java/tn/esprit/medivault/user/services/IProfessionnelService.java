package tn.esprit.medivault.user.services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.medivault.user.dto.DisponibilitesResponse;
import tn.esprit.medivault.user.dto.SignatureResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.TypeProfessionnel;

import java.util.List;

public interface IProfessionnelService {

    List<UtilisateurResponse> rechercher(TypeProfessionnel type, String ville);

    DisponibilitesResponse disponibilites(String id);

    SignatureResponse signature(String medecinId);

    SignatureResponse enregistrerSignature(String medecinId, MultipartFile fichier);
}
