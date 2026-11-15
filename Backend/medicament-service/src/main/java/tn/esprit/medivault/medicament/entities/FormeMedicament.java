package tn.esprit.medivault.medicament.entities;

// enum = liste fermée de valeurs possibles.
// La forme d'un médicament ne peut être QUE l'une de ces 5 valeurs.
// Si le JSON envoie "CAPSULE", Spring refuse la requête (400).
public enum FormeMedicament {
    COMPRIME,
    GELULE,
    SIROP,
    INJECTABLE,
    POMMADE
}
