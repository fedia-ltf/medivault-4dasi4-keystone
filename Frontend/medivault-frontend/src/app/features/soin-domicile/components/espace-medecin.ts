import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { PlanService } from '../services/plan.service';
import { messageErreur } from '../services/soin.service';
import {
  FREQUENCES, PHARMACIES, PlanSuivi, PlanSuiviRequest, STATUTS, TYPES_SOIN, Visite, estAnormale, libelleSoin, nomPharmacie
} from '../models/visite.model';

// Espace médecin : prescrire un suivi à domicile régulier et consulter les mesures relevées
@Component({
  selector: 'mv-espace-medecin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, Icon],
  templateUrl: './espace-medecin.html'
})
export class EspaceMedecin implements OnInit {
  private plans = inject(PlanService);
  private auth = inject(AuthService);

  protected readonly typesSoin = TYPES_SOIN;
  protected readonly frequences = FREQUENCES;
  protected readonly pharmacies = PHARMACIES;
  protected readonly statuts = STATUTS;
  protected readonly libelleSoin = libelleSoin;
  protected readonly nomPharmacie = nomPharmacie;
  protected readonly estAnormale = estAnormale;

  protected readonly mesPlans = signal<PlanSuivi[]>([]);
  protected readonly ouvert = signal<number | null>(null);
  protected readonly visites = signal<Visite[]>([]);
  protected readonly message = signal('');
  protected readonly erreur = signal('');

  protected plan = this.planVide();
  protected seuilTension = '14/9';
  protected seuilGlycemie: number | null = 1.8;

  private get medecinId(): string { return this.auth.utilisateur()?.email ?? ''; }

  ngOnInit(): void { this.charger(); }

  protected charger(): void {
    this.plans.lister(this.medecinId).subscribe({
      next: p => this.mesPlans.set([...p].reverse()),
      error: e => this.afficherErreur(e)
    });
  }

  protected creer(): void {
    const seuils = [
      this.seuilTension ? 'tension>' + this.seuilTension : '',
      this.seuilGlycemie ? 'glycemie>' + this.seuilGlycemie : ''
    ].filter(Boolean).join(';');
    const r: PlanSuiviRequest = { ...this.plan, medecinId: this.medecinId, seuilAlerte: seuils || undefined };
    this.plans.creer(r).subscribe({
      next: p => {
        this.succes(`Suivi prescrit pour ${p.patientId} : les visites sont envoyées à ${nomPharmacie(p.pharmacieId)}.`);
        this.plan = this.planVide();
        this.charger();
        this.voirVisites(p);
      },
      error: e => this.afficherErreur(e)
    });
  }

  protected voirVisites(p: PlanSuivi): void {
    if (this.ouvert() === p.id) { this.ouvert.set(null); return; }
    this.plans.visites(p.id).subscribe({
      next: v => { this.visites.set(v); this.ouvert.set(p.id); },
      error: e => this.afficherErreur(e)
    });
  }

  protected arreter(p: PlanSuivi): void {
    this.plans.arreter(p.id).subscribe({
      next: () => { this.succes('Suivi arrêté : les visites à venir sont annulées.'); this.ouvert.set(null); this.charger(); },
      error: e => this.afficherErreur(e)
    });
  }

  protected enCours(p: PlanSuivi): boolean {
    return p.dateFin >= new Date().toISOString().slice(0, 10);
  }

  protected nbAlertes(p: PlanSuivi): number {
    return this.ouvert() === p.id ? this.visites().filter(v => estAnormale(v, p.seuilAlerte)).length : 0;
  }

  protected libelleFrequence(f: string): string {
    return this.frequences.find(x => x.valeur === f)?.libelle ?? f;
  }

  private planVide(): PlanSuiviRequest {
    return {
      patientId: '', medecinId: '', pharmacieId: PHARMACIES[0].id, adresse: '', typeSoin: 'GLYCEMIE',
      frequence: 'HEBDOMADAIRE', dateDebut: '', dateFin: '', heureVisite: '10:00'
    };
  }

  private succes(m: string): void { this.erreur.set(''); this.message.set(m); }
  private afficherErreur(e: HttpErrorResponse): void { this.message.set(''); this.erreur.set(messageErreur(e)); }
}
