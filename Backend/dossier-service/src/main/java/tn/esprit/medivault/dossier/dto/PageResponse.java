package tn.esprit.medivault.dossier.dto;

import java.util.List;

/**
 * Enveloppe paginée générique renvoyée par les endpoints qui exposent des listes paginées.
 * N'expose pas directement l'objet {@code Page} de Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
