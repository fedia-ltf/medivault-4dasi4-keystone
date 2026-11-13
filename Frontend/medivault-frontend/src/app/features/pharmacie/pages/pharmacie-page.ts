import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { PharmacieScene } from '../components/pharmacie-scene';

// Page principale du module Pharmacie (pharmacie-service).
// Tout ce qui concerne cette page (bandeau, animation, écrans) reste dans ce dossier.
@Component({
  selector: 'mv-pharmacie-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, PharmacieScene],
  template: `
    <section class="enter relative overflow-hidden rounded-[2rem] aurora-dark grain p-8 text-white sm:p-12">
      <div class="absolute inset-0 grid-lines"></div>
      <div class="pointer-events-none absolute inset-y-0 right-0 hidden w-[52%] md:block">
        <mv-pharmacie-scene class="h-full w-full" />
      </div>
      <div class="relative max-w-xl">
        <div class="grid h-14 w-14 place-items-center rounded-2xl bg-aqua-400 text-ink-950">
          <mv-icon name="cross" [size]="26" />
        </div>
        <p class="mt-6 font-mono text-xs text-aqua-300">pharmacie-service</p>
        <h2 class="mt-2 font-display text-6xl leading-none">Pharmacie</h2>
        <p class="mt-4 text-lg text-white/60">Recherche d'une ordonnance par code, délivrance totale ou partielle et historique.</p>
        <div class="mt-8 flex flex-wrap items-center gap-3">
          <span class="chip bg-amber-400/15 px-3 py-1.5 text-amber-300">
            <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-300"></span> En cours de développement
          </span>
        </div>
      </div>
    </section>

    <section class="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      @for (f of fonctions; track f; let i = $index) {
        <article class="card card-hover enter p-5" [style.animation-delay.ms]="120 + i * 80">
          <p class="font-mono text-xs text-ink-300">0{{ i + 1 }}</p>
          <p class="mt-3 font-semibold text-ink-900">{{ f }}</p>
          <p class="mt-1 text-sm text-ink-400">Prévu</p>
        </article>
      }
    </section>

    <a routerLink="/app/dashboard" class="btn btn-soft mt-6 px-5 py-3 text-sm">
      <mv-icon name="chevron-left" [size]="16" /> Retour au tableau de bord
    </a>
  `
})
export class PharmaciePage {
  // Fonctions prévues pour ce module : à remplacer par les vrais écrans
  protected readonly fonctions = ['Recherche par code', 'Délivrance totale ou partielle', 'Blocage des médicaments retirés', 'Événement OrdonnanceDelivree'];
}
