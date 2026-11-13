import { Routes } from '@angular/router';

// Routes du module Ordonnances (préfixe : /app/ordonnances)
export const ORDONNANCES_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Ordonnances' },
    loadComponent: () => import('./pages/ordonnances-page').then(m => m.OrdonnancesPage)
  }
];
