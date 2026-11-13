import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Hologram } from '../../../shared/hologram';

// Animation du module Dossiers médicaux : un cœur en particules qui bat.
// Le moteur de particules (Hologram) est partagé avec la page d'accueil.
@Component({
  selector: 'mv-dossiers-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Hologram],
  host: { class: 'block pointer-events-none' },
  template: `<mv-hologram class="h-full w-full" [fixe]="0" [capsule]="false" [count]="1400" />`
})
export class DossiersScene {}
