import { Routes } from '@angular/router';

// Routes du module Soin à domicile (préfixe : /app/soin-domicile)
export const SOIN_DOMICILE_ROUTES: Routes = [
  {
    path: '',
    data: { title: 'Soin à domicile' },
    loadComponent: () => import('./pages/soin-domicile-page').then(m => m.SoinDomicilePage)
  }
];
