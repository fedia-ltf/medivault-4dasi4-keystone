package tn.esprit.medivault.soindomicile.dto;

/**
 * Corps de POST /api/ventes (medicament-service) : mêmes champs que VenteDtos.Requete.
 * medicament retrouve l'ordonnance par son code (GET /api/ordonnances/code/{code}).
 */
public record VenteRequest(String pharmacieId, String ordonnanceCode, Long medicamentId, int quantite) {
}
