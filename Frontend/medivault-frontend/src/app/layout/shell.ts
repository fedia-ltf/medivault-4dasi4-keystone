import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from './sidebar/sidebar';
import { Topbar } from './topbar/topbar';
import { LayoutService } from './layout.service';

// Cadre de l'espace connecté : menu latéral + en-tête + page du module
@Component({
  selector: 'mv-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Sidebar, Topbar],
  templateUrl: './shell.html'
})
export class Shell {
  protected layout = inject(LayoutService);
}
