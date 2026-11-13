import { Routes } from '@angular/router';

// Routes du module Rendez-vous (préfixe : /app/rendez-vous)
export const RENDEZ_VOUS_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Rendez-vous' },
    loadComponent: () => import('./pages/rendez-vous-page').then(m => m.RendezVousPage)
  }
];
