import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ModuleBanner } from '../../../shared/module-banner';

// Page principale du module Pharmacie (pharmacie-service).
// Le bandeau est commun à tous les modules : ajoute tes écrans en dessous.
@Component({
  selector: 'mv-pharmacie-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModuleBanner],
  template: `<mv-module-banner cle="pharmacie" />`
})
export class PharmaciePage {}
