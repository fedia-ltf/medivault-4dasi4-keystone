import { ChangeDetectionStrategy, Component, input } from '@angular/core';

let seq = 0;

// Logo MediVault : un coffre (cercle) qui protège une croix médicale
@Component({
  selector: 'mv-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex items-center gap-3' },
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 40 40" aria-hidden="true">
      <defs>
        <linearGradient [attr.id]="gid" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#2957cc" />
          <stop offset=".55" stop-color="#4f86f7" />
          <stop offset="1" stop-color="#2cc5b5" />
        </linearGradient>
      </defs>
      <circle cx="20" cy="20" r="18" fill="none" [attr.stroke]="fill" stroke-width="2.2" />
      <circle cx="20" cy="20" r="12.5" fill="none" [attr.stroke]="fill" stroke-width="1" stroke-dasharray="2 3" opacity=".7" />
      <path d="M17 11.5h6v5.5h5.5v6H23v5.5h-6V23h-5.5v-6H17z" [attr.fill]="fill" />
    </svg>
    @if (withText()) {
      <span class="text-[1.35rem] leading-none tracking-tight" [class.text-white]="dark()">
        <span class="font-bold">Medi</span><span class="font-display italic">Vault</span>
      </span>
    }
  `
})
export class Logo {
  protected gid = `mvg-${++seq}`;
  protected fill = `url(#${this.gid})`;
  size = input(36);
  withText = input(true);
  dark = input(false);
}
