import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, input, viewChild
} from '@angular/core';
import { Forme } from '../models/medicament.model';
import { dessiner, fond, palette } from './medicament-render';

// Grande image animée d'un médicament (fiche) : objet en verre brillant qui flotte
// sur un fond bleu profond avec lumière, bokeh et ADN.
@Component({
  selector: 'mv-medicament-visual',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class MedicamentVisual implements AfterViewInit, OnDestroy {
  forme = input.required<Forme>();
  variante = input(0);
  /** Sans fond : seul le médicament est dessiné (pour le poser sur une carte claire) */
  nu = input(false);

  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private frame = 0;
  private resize?: ResizeObserver;

  ngAfterViewInit() {
    const cv = this.canvas().nativeElement;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    let w = 0, h = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const fit = () => {
      w = cv.clientWidth;
      h = cv.clientHeight;
      cv.width = w * dpr;
      cv.height = h * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    this.resize = new ResizeObserver(fit);
    this.resize.observe(cv);
    fit();

    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const t0 = performance.now();
    const draw = (now: number) => {
      const t = reduit ? 1.2 : (now - t0) / 1000;
      const p = palette(this.variante());
      const nu = this.nu();
      if (nu) ctx.clearRect(0, 0, w, h);
      else fond(ctx, w, h, t, p);
      const taille = nu ? Math.min(w * 0.23, h * 0.23) : Math.min(w * 0.15, h * 0.21);
      dessiner(ctx, this.forme(), w / 2, h * (nu ? 0.48 : 0.52) + Math.sin(t * 1.1) * 6, taille, t, p);
      if (!reduit) this.frame = requestAnimationFrame(draw);
    };
    this.zone.runOutsideAngular(() => (this.frame = requestAnimationFrame(draw)));
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.frame);
    this.resize?.disconnect();
  }
}
