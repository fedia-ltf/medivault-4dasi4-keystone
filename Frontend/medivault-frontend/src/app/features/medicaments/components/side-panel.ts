import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { Icon } from '../../../shared/icon';

// Panneau qui glisse depuis la droite (détails, formulaires). À afficher avec @if :
//   @if (ouvert()) { <mv-side-panel titre="…" (fermer)="…">contenu <div pied>boutons</div></mv-side-panel> }
@Component({
  selector: 'mv-side-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="fond fixed inset-0 z-[60] bg-[#0b2545]/30 backdrop-blur-sm" (click)="fermer.emit()"></div>
    <aside class="glisse fixed inset-y-3 right-3 z-[61] flex w-[calc(100%-1.5rem)] max-w-lg flex-col overflow-hidden rounded-[1.75rem] bg-white shadow-2xl" role="dialog" aria-modal="true">
      <header class="flex items-start justify-between gap-4 border-b border-ink-100 px-7 pb-4 pt-6">
        <div>
          @if (surtitre()) { <p class="font-mono text-xs text-aqua-600">{{ surtitre() }}</p> }
          <h3 class="font-display text-3xl leading-tight text-ink-900">{{ titre() }}</h3>
        </div>
        <button (click)="fermer.emit()" class="rounded-xl p-2 text-ink-400 transition hover:bg-ink-50 hover:text-ink-700" aria-label="Fermer"><mv-icon name="x" /></button>
      </header>
      <div class="flex-1 overflow-y-auto px-7 py-5"><ng-content /></div>
      <footer class="empty:hidden border-t border-ink-100 px-7 py-4"><ng-content select="[pied]" /></footer>
    </aside>
  `,
  styles: `
    @keyframes glisse { from { transform: translateX(110%); opacity: 0.4; } to { transform: none; opacity: 1; } }
    @keyframes apparait { from { opacity: 0; } to { opacity: 1; } }
    .glisse { animation: glisse 0.55s cubic-bezier(0.16, 1, 0.3, 1) both; }
    .fond { animation: apparait 0.4s ease both; }
    @media (prefers-reduced-motion: reduce) { .glisse, .fond { animation: none; } }
  `
})
export class SidePanel {
  titre = input.required<string>();
  surtitre = input('');
  fermer = output<void>();

  @HostListener('document:keydown.escape')
  protected echap() { this.fermer.emit(); }
}
