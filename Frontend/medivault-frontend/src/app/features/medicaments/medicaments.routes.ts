import { Routes } from '@angular/router';

// Routes du module Médicaments (préfixe : /app/medicaments)
export const MEDICAMENTS_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Médicaments' },
    loadComponent: () => import('./pages/medicaments-page').then(m => m.MedicamentsPage)
  }
];
