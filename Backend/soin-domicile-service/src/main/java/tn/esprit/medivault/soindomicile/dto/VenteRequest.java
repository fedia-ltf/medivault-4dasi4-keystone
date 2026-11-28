package tn.esprit.medivault.soindomicile.dto;

public record VenteRequest(String pharmacieId, Long medicamentId, Long ordonnanceId, int quantite) {
}
