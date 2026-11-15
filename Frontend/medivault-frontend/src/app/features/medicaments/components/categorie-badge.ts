import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Icon } from '../../../shared/icon';
import { Categorie, categorieInfo } from '../models/sensible.model';

// Badge de catégorie : Stupéfiant (bleu) · Psychotrope (vert d'eau) · Abortif (ciel)
@Component({
  selector: 'mv-categorie',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  host: { class: 'inline-flex' },
  template: `
    <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold ring-1 {{ info().badge }}">
      <mv-icon [name]="info().icone" [size]="12" /> {{ info().label }}
    </span>
  `
})
export class CategorieBadge {
  categorie = input.required<Categorie>();
  protected info = computed(() => categorieInfo(this.categorie()));
}
