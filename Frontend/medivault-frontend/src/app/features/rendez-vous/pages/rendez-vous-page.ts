import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ModuleBanner } from '../../../shared/module-banner';

// Page principale du module Rendez-vous (rendezvous-service).
// Le bandeau est commun à tous les modules : ajoute tes écrans en dessous.
@Component({
  selector: 'mv-rendez-vous-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModuleBanner],
  template: `<mv-module-banner cle="rendez-vous" />`
})
export class RendezVousPage {}
