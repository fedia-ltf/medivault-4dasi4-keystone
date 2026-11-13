// Modules affichés dans le menu latéral (titre et icône).
// Le contenu de chaque page (bandeau, animation, écrans) est dans features/<module>/.
export interface ModuleInfo {
  titre: string;
  icon: string;
}

export const MODULES: Record<string, ModuleInfo> = {
  dossiers: { titre: 'Dossiers médicaux', icon: 'folder' },
  ordonnances: { titre: 'Ordonnances', icon: 'clipboard' },
  pharmacie: { titre: 'Pharmacie', icon: 'cross' },
  analyses: { titre: 'Analyses', icon: 'flask' },
  'rendez-vous': { titre: 'Rendez-vous', icon: 'calendar' }
};
