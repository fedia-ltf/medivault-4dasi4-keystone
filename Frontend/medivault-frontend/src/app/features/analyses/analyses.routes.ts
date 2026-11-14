import { Routes } from '@angular/router';

// Routes du module Analyses (préfixe : /app/analyses)
export const ANALYSES_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Analyses' },
    loadComponent: () => import('./pages/analyses-page').then(m => m.AnalysesPage)
  }
];
