import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { Icon } from '../../shared/icon';
import { AuthService } from '../../core/auth.service';
import { LayoutService } from '../layout.service';

// En-tête commun : titre de la page, recherche, heure, notifications, profil
@Component({
  selector: 'mv-topbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'contents' },
  imports: [DatePipe, Icon],
  templateUrl: './topbar.html'
})
export class Topbar {
  protected auth = inject(AuthService);
  protected layout = inject(LayoutService);
  private router = inject(Router);

  protected notifOuvert = signal(false);
  protected titre = signal('MediVault');
  protected heure = signal(new Date());

  protected readonly notifications = [
    { icon: 'alert', ton: 'text-amber-600 bg-amber-50', titre: 'Interaction GRAVE détectée', texte: 'Coumadine + Advil · ordonnance ORD-A7F3', quand: 'il y a 4 min' },
    { icon: 'check', ton: 'text-emerald-600 bg-emerald-50', titre: 'Ordonnance délivrée', texte: 'Pharmacie Centrale · Ahmed T.', quand: 'il y a 22 min' },
    { icon: 'flask', ton: 'text-iris-500 bg-indigo-50', titre: 'Résultat disponible', texte: 'Bilan lipidique · Sarra M.', quand: 'il y a 1 h' }
  ];

  constructor() {
    // Le titre vient de « data: { title } » de la route active
    const majTitre = () => {
      let r = this.router.routerState.snapshot.root;
      while (r.firstChild) r = r.firstChild;
      this.titre.set(r.data['title'] ?? 'MediVault');
      this.notifOuvert.set(false);
    };
    majTitre();
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(majTitre);

    const timer = setInterval(() => this.heure.set(new Date()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}
