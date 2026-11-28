package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.medivault.medicament.entities.Categorie;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;

// DTO = objet qui voyage entre le client (Angular) et le service : on n'expose jamais directement les entités.
// Un "record" Java est une classe de données immuable (Java 17).
public final class MedicamentSensibleDtos {

    private MedicamentSensibleDtos() {
    }

    // Ce que l'administrateur envoie pour créer ou modifier un médicament sensible
    public record Requete(
            @NotBlank(message = "Le nom commercial est obligatoire") @Size(max = 120) String nomCommercial,
            @NotBlank(message = "La DCI est obligatoire") @Size(max = 120) String dci,
            @NotNull(message = "La catégorie est obligatoire") Categorie categorie,
            @Size(max = 1000) String description,
            @Size(max = 1000) String precautions) {
    }

    // Ce que le service renvoie
    public record Reponse(Long id, String nomCommercial, String dci, Categorie categorie,
                          String description, String precautions) {

        public static Reponse de(MedicamentSensible m) {
            return new Reponse(m.getId(), m.getNomCommercial(), m.getDci(), m.getCategorie(),
                    m.getDescription(), m.getPrecautions());
        }
    }
}