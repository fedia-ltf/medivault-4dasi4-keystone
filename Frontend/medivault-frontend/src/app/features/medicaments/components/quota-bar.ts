import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

// Barre « vendu / attribué » : devient bleu marine au-dessus de 90 %, rouge à 100 %.
@Component({
  selector: 'mv-quota-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="flex items-center justify-between text-xs">
      <span class="font-mono text-ink-500"><span class="font-bold text-ink-800">{{ vendu() }}</span> / {{ attribue() }}</span>
      <span class="font-bold {{ taux() >= 100 ? 'text-rose-600' : taux() > 90 ? 'text-aqua-700' : 'text-ink-400' }}">{{ taux() }} %</span>
    </div>
    <div class="mt-1.5 overflow-hidden rounded-full bg-ink-50" [style.height.px]="fin() ? 6 : 9">
      <div class="h-full rounded-full transition-[width] duration-1000 ease-out-expo {{ taux() >= 100 ? 'bg-rose-500' : taux() > 90 ? 'bg-aqua-700' : 'bg-gradient-to-r from-aqua-500 to-iris-400' }}"
           [style.width.%]="pret() ? min100() : 0"></div>
    </div>
  `
})
export class QuotaBar {
  vendu = input.required<number>();
  attribue = input.required<number>();
  fin = input(false);
  protected pret = signal(false);
  protected taux = computed(() => (this.attribue() ? Math.round((this.vendu() / this.attribue()) * 100) : 0));
  protected min100 = computed(() => Math.min(100, this.taux()));

  constructor() {
    setTimeout(() => this.pret.set(true), 120);
  }
}
