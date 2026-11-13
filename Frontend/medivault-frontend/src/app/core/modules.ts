// Liste des modules de la plateforme : sert au menu latéral et au bandeau de chaque module.
// Pour changer le titre, l'icône ou la description de ton module, c'est ici.
export interface ModuleInfo {
  titre: string;
  icon: string;
  /** Animation du bandeau, propre à chaque module */
  anim: 'coeur' | 'docs' | 'croix' | 'adn' | 'orbite';
  owner: string;
  service: string;
  description: string;
  fonctions: string[];
}

export const MODULES: Record<string, ModuleInfo> = {
  dossiers: {
    titre: 'Dossiers médicaux', icon: 'folder', anim: 'coeur', owner: 'Eya', service: 'dossier-service',
    description: 'Le dossier du patient : antécédents, allergies, consultations et accès accordés aux soignants.',
    fonctions: ['Dossier patient et allergies', 'Consultations', 'Gestion des accès médecin', 'Événement ConsultationAjoutee']
  },
  ordonnances: {
    titre: 'Ordonnances', icon: 'clipboard', anim: 'docs', owner: 'Amine', service: 'ordonnance-service',
    description: 'Rédaction des ordonnances avec vérification de compatibilité auprès du medicament-service.',
    fonctions: ['Rédaction guidée', 'Alertes allergies et interactions', 'Code unique par ordonnance', 'Événement OrdonnanceCreee']
  },
  pharmacie: {
    titre: 'Pharmacie', icon: 'cross', anim: 'croix', owner: 'Aziz', service: 'pharmacie-service',
    description: 'Recherche d\'une ordonnance par code, délivrance totale ou partielle et historique.',
    fonctions: ['Recherche par code', 'Délivrance totale ou partielle', 'Blocage des médicaments retirés', 'Événement OrdonnanceDelivree']
  },
  analyses: {
    titre: 'Analyses', icon: 'flask', anim: 'adn', owner: 'Taha', service: 'analyse-service',
    description: 'Demandes d\'analyses, saisie des résultats par le laborantin et diffusion au dossier.',
    fonctions: ['Analyses à traiter', 'Saisie des résultats', 'Mes résultats (patient)', 'Événement ResultatDisponible']
  },
  'rendez-vous': {
    titre: 'Rendez-vous', icon: 'calendar', anim: 'orbite', owner: 'Yazid', service: 'rendezvous-service',
    description: 'Agenda des médecins, prise et confirmation des rendez-vous, annulations.',
    fonctions: ['Agenda du médecin', 'Prise de rendez-vous', 'Confirmation et annulation', 'Événement RendezVousConfirme']
  }
};
