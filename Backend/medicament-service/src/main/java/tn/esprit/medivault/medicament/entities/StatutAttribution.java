package tn.esprit.medivault.medicament.entities;

// Etat d'un quota. ACTIVE : la pharmacie peut vendre. SUSPENDUE : plus aucune vente de ce médicament
// jusqu'à ce que l'administrateur lève la sanction (RG3 : suspension au 2e avertissement).
public enum StatutAttribution {
    ACTIVE,
    SUSPENDUE
}