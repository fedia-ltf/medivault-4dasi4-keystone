import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ModuleBanner } from '../../../shared/module-banner';

// Page principale du module Ordonnances (ordonnance-service).
// Le bandeau est commun à tous les modules : ajoute tes écrans en dessous.
@Component({
  selector: 'mv-ordonnances-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModuleBanner],
  template: `<mv-module-banner cle="ordonnances" />`
})
export class OrdonnancesPage {}
