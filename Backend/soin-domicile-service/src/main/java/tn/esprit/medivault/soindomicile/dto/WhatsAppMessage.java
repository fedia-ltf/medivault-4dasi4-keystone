package tn.esprit.medivault.soindomicile.dto;

/**
 * Message WhatsApp destiné à l'infirmier affecté à une visite.
 *
 * @param telephone            numéro de l'infirmier, tel qu'il a été saisi
 * @param message              texte du message (infos du patient et du soin)
 * @param lien                 lien wa.me qui ouvre WhatsApp avec le message déjà écrit
 * @param envoiAutomatiqueActif vrai si la clé CallMeBot est configurée : le message part tout seul à l'affectation
 */
public record WhatsAppMessage(String telephone, String message, String lien, boolean envoiAutomatiqueActif) {
}
