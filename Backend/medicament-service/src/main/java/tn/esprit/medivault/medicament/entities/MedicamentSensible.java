package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medicament_sensible")
@Getter
@Setter
//Lombok yekteb f blasti les getNomCommercial() et setNomCommercial()
@NoArgsConstructor
//Un constructeur vide, obligatoire pour JPA.

@AllArgsConstructor
//Un constructeur avec tous les champs.

@Builder
public class MedicamentSensible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nomCommercial;

    @Column(nullable = false)
    private String dci;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String precautions;
}