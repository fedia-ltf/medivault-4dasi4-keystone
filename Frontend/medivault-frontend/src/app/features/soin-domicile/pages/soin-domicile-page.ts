import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { SoinDomicileScene } from '../components/soin-domicile-scene';
import { EspacePatient } from '../components/espace-patient';
import { EspacePharmacien } from '../components/espace-pharmacien';
import { EspaceMedecin } from '../components/espace-medecin';

// Une seule page « Soin à domicile » : son contenu dépend du rôle de l'utilisateur connecté
@Component({
  selector: 'mv-soin-domicile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, SoinDomicileScene, EspacePatient, EspacePharmacien, EspaceMedecin],
  templateUrl: './soin-domicile-page.html'
})
export class SoinDomicilePage {
  private auth = inject(AuthService);

  protected readonly role = computed(() => this.auth.utilisateur()?.role);

  protected readonly intro = computed(() => {
    switch (this.role()) {
      case 'PATIENT': return 'Demandez un infirmier à domicile à votre pharmacie, puis suivez sa confirmation et l\'infirmier affecté.';
      case 'PHARMACIEN': return 'Confirmez les demandes des patients, affectez un infirmier de la pharmacie et saisissez son rapport : il rejoint le dossier médical.';
      case 'MEDECIN': return 'Prescrivez un suivi à domicile régulier et consultez les mesures relevées chez vos patients.';
      default: return 'Le soin à domicile concerne les patients, les pharmaciens et les médecins.';
    }
  });

  protected readonly espace = computed(() => {
    switch (this.role()) {
      case 'PATIENT': return 'Espace patient';
      case 'PHARMACIEN': return 'Espace pharmacien';
      case 'MEDECIN': return 'Espace médecin';
      default: return '';
    }
  });
}
