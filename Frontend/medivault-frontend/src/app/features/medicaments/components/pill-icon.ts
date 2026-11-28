import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Forme } from '../models/medicament.model';
import { sprite } from './medicament-render';

// Petite image d'un médicament selon sa forme (gélule, comprimé, flacon, ampoule, tube).
// `variante` (ex. l'id du médicament) change la couleur : deux médicaments voisins ne se ressemblent pas.
@Component({
  selector: 'mv-pill-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex shrink-0' },
  template: `<img [src]="src()" [attr.width]="size()" [attr.height]="size()" alt="" draggable="false" />`
})
export class PillIcon {
  forme = input.required<Forme>();
  variante = input(0);
  size = input(40);
  protected src = computed(() => sprite(this.forme(), this.variante(), 'icone'));
}
