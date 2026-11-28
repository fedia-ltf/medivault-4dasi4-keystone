import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { Hologram } from '../../shared/hologram';

// Le cadre commun des pages d'inscription et de mot de passe oublié : le même panneau visuel que la page de connexion,
// et le formulaire de la page dans la zone de droite.
@Component({
  selector: 'mv-auth-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, Logo, Hologram],
  template: `
    <div class="grid min-h-[100svh] lg:grid-cols-[1.05fr_1fr]">
      <aside class="relative hidden overflow-hidden aurora-dark grain text-white lg:flex lg:flex-col lg:justify-between p-12">
        <div class="absolute inset-0 grid-lines"></div>
        <div class="absolute -right-32 top-10 h-[520px] w-[520px] rounded-full bg-aqua-500/25 blur-[120px] animate-drift"></div>
        <div class="absolute -left-20 bottom-0 h-[420px] w-[420px] rounded-full bg-iris-500/20 blur-[110px] animate-drift [animation-delay:-10s]"></div>

        <a routerLink="/" class="relative z-10 enter"><mv-logo [dark]="true" /></a>

        <div class="absolute inset-y-0 right-[-16%] w-[60%]">
          <mv-hologram class="h-full w-full" [capsule]="false" [count]="1500" />
        </div>

        <div class="relative z-10 max-w-md enter [animation-delay:150ms]">
          <p class="text-xs font-bold uppercase tracking-[0.3em] text-aqua-300">Espace sécurisé</p>
          <h1 class="mt-5 font-display text-6xl leading-[1]">
            Chaque donnée,<br /><span class="italic text-gradient">au bon soignant.</span>
          </h1>
          <p class="mt-6 text-white/60 leading-relaxed">
            Médecins, patients, pharmaciens, laborantins et administrateurs partagent un même coffre,
            chacun avec ses propres droits.
          </p>
        </div>

        <div class="relative z-10 flex items-center gap-6 text-xs text-white/45 enter [animation-delay:300ms]">
          <span class="flex items-center gap-2"><mv-icon name="shield-check" [size]="15" class="text-aqua-300" /> OAuth2 · JWT</span>
          <span class="flex items-center gap-2"><mv-icon name="lock" [size]="15" class="text-aqua-300" /> Données chiffrées</span>
        </div>
      </aside>

      <main class="relative flex items-center justify-center overflow-hidden aurora-light px-6 py-12">
        <div class="absolute right-[-10%] top-[-10%] h-80 w-80 rounded-full bg-aqua-300/30 blur-3xl"></div>
        <div class="relative w-full max-w-md">
          <a routerLink="/" class="mb-10 inline-flex lg:hidden"><mv-logo /></a>
          <ng-content />
        </div>
      </main>
    </div>
  `
})
export class AuthLayout {}
