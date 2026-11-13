import { AfterViewInit, Directive, ElementRef, OnDestroy, inject, input } from '@angular/core';

// Ajoute la classe "is-visible" quand l'élément entre à l'écran (animation au défilement)
@Directive({ selector: '[mvReveal]', host: { class: 'reveal' } })
export class Reveal implements AfterViewInit, OnDestroy {
  delay = input(0, { alias: 'mvReveal', transform: (v: unknown) => Number(v) || 0 });
  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  ngAfterViewInit() {
    const node = this.el.nativeElement as HTMLElement;
    node.style.transitionDelay = `${this.delay()}ms`;
    this.observer = new IntersectionObserver(
      entries => entries.forEach(e => {
        if (e.isIntersecting) {
          node.classList.add('is-visible');
          this.observer?.disconnect();
        }
      }),
      { threshold: 0.15 }
    );
    this.observer.observe(node);
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}
