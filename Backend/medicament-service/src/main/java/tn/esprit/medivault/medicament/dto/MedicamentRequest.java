package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.medicament.entities.FormeMedicament;

// DTO d'entrée : le JSON que l'admin envoie pour AJOUTER un médicament (POST /api/medicaments).
// Pourquoi pas directement l'entité Medicament ?
//   - l'admin ne doit pas choisir l'id (c'est la base qui le donne)
//   - il ne doit pas toucher nb_delivrances (calculé par les événements)
//   - on met ici les règles de validation, sans salir l'entité
// Les annotations de validation sont vérifiées grâce à @Valid dans le controller.
// Si une règle n'est pas respectée -> 400 Bad Request avec le message ci-dessous.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentRequest {

    // @NotBlank : ni null, ni vide, ni que des espaces. @Size(max=100) : 100 caractères max.
    @NotBlank(message = "Le nom commercial est obligatoire")
    @Size(max = 100)
    private String nom_commercial;

    @NotBlank(message = "La DCI est obligatoire")
    @Size(max = 100)
    private String dci;

    @NotBlank(message = "La classe thérapeutique est obligatoire")
    @Size(max = 100)
    private String classe_therapeutique;

    // @NotNull : pour un enum on vérifie juste qu'il est présent
    @NotNull(message = "La forme est obligatoire (COMPRIME, GELULE, SIROP, INJECTABLE, POMMADE)")
    private FormeMedicament forme;

    // Facultatif (pas de @NotBlank)
    @Size(max = 50)
    private String dosage;
}
