import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Icon } from '../../../shared/icon';
import { MedicamentsScene } from './medicaments-scene';

// Bannière commune de mes pages. La gélule 3D et les étiquettes sont toujours les mêmes ;
// seules les couleurs changent entre la version claire (par défaut) et la version « hero--dark ».
//
// Pour revenir à l'ancienne version bleu profond : <mv-hero [sombre]="true" …>
// (ou ajouter la classe CSS « hero--dark » sur la section).
@Component({
  selector: 'mv-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, MedicamentsScene],
  template: `
    <section class="hero enter relative overflow-hidden rounded-[2rem] px-8 py-9 sm:min-h-[19rem] sm:px-12" [class.hero--dark]="sombre()">
      <mv-medicaments-scene class="absolute inset-0" [chips]="chips()" [sombre]="sombre()" />
      <div class="relative max-w-md">
        <div class="hero-icone grid h-11 w-11 place-items-center rounded-xl"><mv-icon [name]="icone()" [size]="22" /></div>
        <p class="hero-eyebrow mt-4 font-mono text-xs uppercase tracking-[0.18em]">{{ eyebrow() }}</p>
        <h2 class="hero-titre mt-2 font-display text-5xl leading-[0.95] sm:text-6xl">{{ titre() }}</h2>
        <p class="hero-phrase mt-3 text-base leading-relaxed">{{ phrase() }}</p>
        <div class="mt-5 flex flex-wrap items-center gap-3"><ng-content /></div>
      </div>
    </section>
  `,
  styles: `
    /* ---- Version claire : dégradé blanc → bleu pâle avec une touche vert d'eau, sans grille ---- */
    .hero {
      background: linear-gradient(120deg, #F7FAFF 0%, #EAF1FF 55%, #E6F6F1 100%);
      border: 1px solid #ffffff;
      box-shadow: 0 36px 70px -44px rgba(41, 87, 204, 0.55), 0 1px 0 #ffffff inset;
    }
    .hero .hero-icone { background: #2957cc; color: #ffffff; box-shadow: 0 14px 24px -12px rgba(41, 87, 204, 0.9); }
    .hero .hero-eyebrow { color: #2957cc; }
    .hero .hero-titre { color: #12306b; }
    .hero .hero-phrase { color: #5a6b7c; }

    /* ---- Ancienne version (bleu profond) : on la garde ici pour pouvoir y revenir ---- */
    .hero--dark {
      background: linear-gradient(120deg, #12306b, #2957cc 55%, #4f86f7);
      border-color: transparent;
      box-shadow: 0 36px 70px -36px rgba(41, 87, 204, 0.85);
    }
    .hero--dark .hero-icone { background: rgba(255, 255, 255, 0.15); color: #ffffff; box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.3); }
    .hero--dark .hero-eyebrow { color: #cfe2ff; }
    .hero--dark .hero-titre { color: #ffffff; }
    .hero--dark .hero-phrase { color: rgba(255, 255, 255, 0.75); }
  `
})
export class Hero {
  eyebrow = input.required<string>();
  titre = input.required<string>();
  phrase = input.required<string>();
  icone = input('pill');
  chips = input(true);
  /** true = ancienne version bleu profond (« hero--dark ») */
  sombre = input(false);
}
