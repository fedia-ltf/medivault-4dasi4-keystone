package tn.esprit.medivault.soindomicile.dto;

import java.util.Map;

/**
 * Chiffres du back-office administrateur (GET /api/visites/statistiques).
 * Les Map associent une clé (statut, type de soin, pharmacie, infirmier) à un nombre.
 */
public record StatistiquesSoin(
        long totalDemandes,
        long enAttente,
        long confirmees,
        long realisees,
        long annulees,
        int tauxRealisation,                      // % des demandes traitées (réalisées / (réalisées + annulées))
        long totalInfirmiers,
        long totalPlans,
        long plansEnCours,
        Map<String, Long> parStatut,
        Map<String, Long> parType,
        Map<String, Long> demandesParPharmacie,
        Map<String, Long> infirmiersParPharmacie,
        Map<String, Long> topInfirmiers) {
}
