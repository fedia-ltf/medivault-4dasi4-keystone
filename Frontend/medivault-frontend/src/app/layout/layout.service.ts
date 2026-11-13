import { Injectable, signal } from '@angular/core';

// État partagé entre le menu latéral, l'en-tête et la page
@Injectable({ providedIn: 'root' })
export class LayoutService {
  /** Menu latéral réduit (icônes seules) */
  readonly replie = signal(false);
  /** Menu ouvert sur mobile */
  readonly menuMobile = signal(false);
}
