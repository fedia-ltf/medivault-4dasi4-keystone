package tn.esprit.medivault.user.dto;

/**
 * Signature du médecin, lue par ordonnance-service pour l'afficher sur l'ordonnance.
 * signatureImage : data URL (ex. "data:image/png;base64,iVBOR…").
 */
public record SignatureResponse(
        String medecinId,
        String nomComplet,
        String signatureImage
) {
}
