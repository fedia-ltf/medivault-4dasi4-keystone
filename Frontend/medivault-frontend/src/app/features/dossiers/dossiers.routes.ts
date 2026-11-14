import { Routes } from '@angular/router';

// Routes du module Dossiers médicaux (préfixe : /app/dossiers)
export const DOSSIERS_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Dossiers médicaux' },
    loadComponent: () => import('./pages/dossiers-page').then(m => m.DossiersPage)
  }
];
