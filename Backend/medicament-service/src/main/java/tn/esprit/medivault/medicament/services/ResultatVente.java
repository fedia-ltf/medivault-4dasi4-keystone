package tn.esprit.medivault.medicament.services;

import org.springframework.http.HttpStatus;
import tn.esprit.medivault.medicament.dto.VenteDtos;
import tn.esprit.medivault.medicament.entities.Attribution;

// Résultat d'une déclaration de vente : soit la vente enregistrée, soit un refus expliqué.
// Un refus n'est PAS une exception : en cas de faute, l'avertissement doit être enregistré en base
// (une exception annulerait la transaction et l'avertissement serait perdu).
public record ResultatVente(VenteDtos.Reponse vente, Refus refus) {

    public record Refus(HttpStatus statut, String titre, String motif, int avertissements, boolean suspendue) {
    }

    public static ResultatVente accepte(VenteDtos.Reponse vente) {
        return new ResultatVente(vente, null);
    }

    // a peut être null (aucun quota) : dans ce cas aucun avertissement n'est compté
    public static ResultatVente refus(HttpStatus statut, String titre, String motif, Attribution a) {
        return new ResultatVente(null, new Refus(statut, titre, motif,
                a == null ? 0 : a.getAvertissements(), a != null && a.estSuspendue()));
    }

    public boolean estAccepte() {
        return vente != null;
    }
}