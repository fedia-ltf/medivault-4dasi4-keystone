import { Routes } from '@angular/router';

// Routes du module Pharmacie (préfixe : /app/pharmacie)
export const PHARMACIE_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Pharmacie' },
    loadComponent: () => import('./pages/pharmacie-page').then(m => m.PharmaciePage)
  }
];
