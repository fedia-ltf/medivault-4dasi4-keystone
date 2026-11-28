package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Une vente de médicament sensible, justifiée par une ordonnance.
// Règle de la documentation : une VenteSensible n'est JAMAIS modifiée ni supprimée (traçabilité).
// Le pharmacien et le médicament se retrouvent par l'attribution (le quota consommé).
@Entity
@Table(name = "vente_sensible")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenteSensible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le patient vient de l'ordonnance (identifiant texte, comme dans user-service)
    @Column(nullable = false)
    private String patientId;

    // Identifiant de l'ordonnance dans ordonnance-service
    @Column(nullable = false)
    private Long ordonnanceId;

    // Attribut ajouté : le code lisible de l'ordonnance (ex. ORD-2026-0123), pratique pour l'affichage
    private String ordonnanceCode;

    @Column(nullable = false)
    private int quantite;

    @Column(nullable = false)
    private LocalDateTime date;

    // Un quota contient 0..* ventes
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribution_id", nullable = false)
    private Attribution attribution;
}