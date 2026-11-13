import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ModuleBanner } from '../../../shared/module-banner';

// Page principale du module Dossiers médicaux (dossier-service).
// Le bandeau est commun à tous les modules : ajoute tes écrans en dessous.
@Component({
  selector: 'mv-dossiers-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModuleBanner],
  template: `<mv-module-banner cle="dossiers" />`
})
export class DossiersPage {}
