package tn.esprit.medivault.user.services;

import tn.esprit.medivault.user.dto.UtilisateurRequest;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;

import java.util.List;

public interface IUtilisateurService {

    UtilisateurResponse creer(UtilisateurRequest request);

    UtilisateurResponse trouverParId(String id);

    List<UtilisateurResponse> lister(Role role);

    UtilisateurResponse changerStatut(String id, boolean actif);
}
