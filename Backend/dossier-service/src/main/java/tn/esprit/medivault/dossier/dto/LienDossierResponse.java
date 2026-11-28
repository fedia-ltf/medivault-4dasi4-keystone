package tn.esprit.medivault.dossier.dto;

import tn.esprit.medivault.dossier.entities.TypeLien;

import java.time.LocalDateTime;

public record LienDossierResponse(
        Long id,
        TypeLien type,
        String referenceId,
        LocalDateTime date,
        String auteurId,
        String libelle
) {}
