package tn.esprit.medivault.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.entities.Utilisateur;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Réponse unique pour un patient, un administrateur ou un professionnel :
 * les champs professionnels sont absents (non sérialisés) pour un patient.
 * La signature n'est pas incluse ici : voir GET /api/professionnels/{id}/signature.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtilisateurResponse(
        String id,
        String nom,
        String prenom,
        String email,
        String telephone,
        Role role,
        boolean actif,
        TypeProfessionnel type,
        String nomEtablissement,
        String numeroAgrement,
        String adresse,
        String ville,
        List<DayOfWeek> joursTravail,
        LocalTime heureDebut,
        LocalTime heureFin,
        Boolean signatureDisponible
) {

    public static UtilisateurResponse from(Utilisateur u) {
        if (u instanceof Professionnel p) {
            Boolean signature = p.getType() == TypeProfessionnel.MEDECIN ? p.getSignatureImage() != null : null;
            return new UtilisateurResponse(p.getId(), p.getNom(), p.getPrenom(), p.getEmail(), p.getTelephone(),
                    p.getRole(), p.isActif(), p.getType(), p.getNomEtablissement(), p.getNumeroAgrement(),
                    p.getAdresse(), p.getVille(), p.getJoursTravail(), p.getHeureDebut(), p.getHeureFin(), signature);
        }
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getRole(), u.isActif(), null, null, null, null, null, null, null, null, null);
    }
}
