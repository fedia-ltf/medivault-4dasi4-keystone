import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { AuthService, ROLES } from '../../core/auth.service';
import { MODULES } from '../../core/modules';
import { LayoutService } from '../layout.service';

// Menu latéral commun à toutes les pages de l'espace connecté.
// Les liens viennent de core/modules.ts : pas besoin de modifier ce fichier pour ajouter une page.
@Component({
  selector: 'mv-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'contents' },
  imports: [RouterLink, RouterLinkActive, Icon, Logo],
  templateUrl: './sidebar.html'
})
export class Sidebar {
  protected auth = inject(AuthService);
  protected layout = inject(LayoutService);
  private router = inject(Router);

  protected roleLabel = computed(() => ROLES.find(r => r.value === this.auth.utilisateur()?.role)?.label ?? '');

  protected readonly navigation = [
    { lien: '/app/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { lien: '/app/medicaments', icon: 'pill', label: 'Médicaments' },
    ...Object.entries(MODULES).map(([cle, m]) => ({ lien: `/app/${cle}`, icon: m.icon, label: m.titre }))
  ];

  constructor() {
    // Sur mobile, le menu se referme après chaque navigation
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.layout.menuMobile.set(false));
  }

  protected deconnexion() {
    this.auth.deconnexion();
    this.router.navigateByUrl('/login');
  }
}
