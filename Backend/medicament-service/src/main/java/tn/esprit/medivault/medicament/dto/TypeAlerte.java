package tn.esprit.medivault.medicament.dto;

// Les 3 types d'alerte possibles dans la vérification de compatibilité
public enum TypeAlerte {
    ALLERGIE,          // RG4 : le patient est allergique à la classe du médicament
    INTERACTION,       // RG4 : deux médicaments de la liste interagissent
    MEDICAMENT_RETIRE  // RG6 : le médicament n'existe plus dans le catalogue
}
