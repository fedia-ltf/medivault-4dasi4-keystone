package tn.esprit.medivault.medicament.entities;

// Niveau de danger d'une interaction entre deux médicaments.
// Seule CONTRE_INDIQUEE bloque la prescription (RG4) ;
// les autres (dont GRAVE) donnent seulement une alerte.
public enum Gravite {
    FAIBLE,
    MODEREE,
    GRAVE,
    CONTRE_INDIQUEE
}
