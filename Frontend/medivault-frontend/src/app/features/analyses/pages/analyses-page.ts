import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ModuleBanner } from '../../../shared/module-banner';

// Page principale du module Analyses (analyse-service).
// Le bandeau est commun à tous les modules : ajoute tes écrans en dessous.
@Component({
  selector: 'mv-analyses-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModuleBanner],
  template: `<mv-module-banner cle="analyses" />`
})
export class AnalysesPage {}
