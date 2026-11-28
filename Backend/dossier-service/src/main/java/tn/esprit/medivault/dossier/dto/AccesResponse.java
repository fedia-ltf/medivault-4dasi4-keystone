package tn.esprit.medivault.dossier.dto;

import tn.esprit.medivault.dossier.entities.StatutAcces;

import java.time.LocalDateTime;

public record AccesResponse(
        Long id,
        String professionnelId,
        StatutAcces statut,
        LocalDateTime dateAccord,
        LocalDateTime dateRetrait
) {}
