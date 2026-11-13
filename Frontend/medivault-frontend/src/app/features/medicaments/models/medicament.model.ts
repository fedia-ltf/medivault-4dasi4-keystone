// Modèles du module Médicaments (mêmes noms que le JSON du medicament-service)
export type Forme = 'COMPRIME' | 'GELULE' | 'SIROP' | 'INJECTABLE' | 'POMMADE';

export interface Medicament {
  id: number;
  nom_commercial: string;
  dci: string;
  classe_therapeutique: string;
  forme: Forme;
  dosage: string;
  nb_delivrances?: number;
}

export const FORMES: Forme[] = ['COMPRIME', 'GELULE', 'SIROP', 'INJECTABLE', 'POMMADE'];
