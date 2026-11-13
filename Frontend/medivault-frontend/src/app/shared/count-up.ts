import { AfterViewInit, Directive, ElementRef, OnDestroy, effect, inject, input } from '@angular/core';

// Compteur animé : de 0 jusqu'à la valeur, quand l'élément devient visible
@Directive({ selector: '[mvCountUp]' })
export class CountUp implements AfterViewInit, OnDestroy {
  value = input.required<number>({ alias: 'mvCountUp' });
  duration = input(1800);
  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;
  private started = false;
  private format = new Intl.NumberFormat('fr-FR');

  constructor() {
    // Si la valeur change après l'animation (ex. réponse de l'API), on rejoue
    effect(() => {
      const v = this.value();
      if (this.started) this.animate(v);
    });
  }

  ngAfterViewInit() {
    const node = this.el.nativeElement as HTMLElement;
    node.textContent = '0';
    this.observer = new IntersectionObserver(entries => {
      if (entries.some(e => e.isIntersecting)) {
        this.started = true;
        this.animate(this.value());
        this.observer?.disconnect();
      }
    });
    this.observer.observe(node);
  }

  private animate(target: number) {
    const node = this.el.nativeElement as HTMLElement;
    const start = performance.now();
    const step = (now: number) => {
      const t = Math.min((now - start) / this.duration(), 1);
      const eased = 1 - Math.pow(1 - t, 4);
      node.textContent = this.format.format(Math.round(target * eased));
      if (t < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}
