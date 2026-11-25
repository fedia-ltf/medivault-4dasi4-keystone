import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { SoinService, messageErreur } from '../services/soin.service';
import { LocalisationService } from '../services/localisation.service';
import {
  PHARMACIES, RechercheVisites, STATUTS, StatutVisite, TYPES_SOIN, Visite, VisiteRequest, libelleSoin, lienCarte, nomPharmacie,
  ordonnanceObligatoire
} from '../models/visite.model';

// Espace patient : demander un soin à domicile, suivre sa confirmation et l'infirmier affecté
@Component({
  selector: 'mv-espace-patient',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, Icon],
  templateUrl: './espace-patient.html'
})
export class EspacePatient implements OnInit {
  private soin = inject(SoinService);
  private auth = inject(AuthService);
  private localisation = inject(LocalisationService);
  private destroyRef = inject(DestroyRef);

  protected readonly typesSoin = TYPES_SOIN;
  protected readonly pharmacies = PHARMACIES;
  protected readonly statuts = STATUTS;
  protected readonly libelleSoin = libelleSoin;
  protected readonly nomPharmacie = nomPharmacie;
  protected readonly lienCarte = lienCarte;

  protected readonly visites = signal<Visite[]>([]);
  protected readonly message = signal('');
  protected readonly erreur = signal('');
  // État de la localisation automatique : '' | 'recherche' | 'ok' | message d'erreur
  protected readonly etatPosition = signal('');

  // ---------- Recherche dans mes demandes (/api/visites/recherche?patientId=...) ----------
  protected readonly listeStatuts = Object.entries(STATUTS).map(([valeur, s]) => ({ valeur: valeur as StatutVisite, libelle: s.libelle }));
  protected filtre: RechercheVisites = this.filtreVide();
  protected readonly resultats = signal<Visite[] | null>(null);       // null = pas de recherche
  private readonly recherche$ = new Subject<RechercheVisites>();

  // Les demandes les plus récentes en premier
  protected readonly mesVisites = computed(() =>
    [...(this.resultats() ?? this.visites())].sort((a, b) => b.date.localeCompare(a.date)));

  protected demande: VisiteRequest = this.demandeVide();

  private get patientId(): string { return this.auth.utilisateur()?.email ?? ''; }

  ngOnInit(): void {
    this.recherche$.pipe(
      debounceTime(300),
      switchMap(f => this.soin.rechercher(f).pipe(catchError(e => { this.afficherErreur(e); return EMPTY; }))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(v => this.resultats.set(v));
    this.charger();
    this.localiser();             // l'adresse se remplit toute seule avec la position actuelle
  }

  /** Position actuelle du patient : remplit l'adresse et joint les coordonnées GPS. */
  protected localiser(): void {
    this.etatPosition.set('recherche');
    this.localisation.positionActuelle()
      .then(p => {
        this.demande.adresse = p.adresse;
        this.demande.latitude = p.latitude;
        this.demande.longitude = p.longitude;
        this.etatPosition.set('ok');
      })
      .catch((e: Error) => this.etatPosition.set(e.message));
  }

  protected retirerPosition(): void {
    this.demande.latitude = undefined;
    this.demande.longitude = undefined;
    this.etatPosition.set('');
  }

  protected charger(): void {
    this.soin.lister({ patientId: this.patientId }).subscribe({
      next: v => this.visites.set(v),
      error: e => this.afficherErreur(e)
    });
    this.rechercher();
  }

  /** Recherche dans MES demandes seulement : le patientId est toujours envoyé. */
  protected rechercher(): void {
    const { q, statut, typeSoin } = this.filtre;
    if (!q?.trim() && !statut && !typeSoin) { this.resultats.set(null); return; }
    this.recherche$.next({ ...this.filtre, patientId: this.patientId });
  }

  protected effacerRecherche(): void {
    this.filtre = this.filtreVide();
    this.resultats.set(null);
  }

  private filtreVide(): RechercheVisites { return { q: '', statut: '', typeSoin: '' }; }

  protected ordonnanceObligatoire(): boolean { return ordonnanceObligatoire(this.demande.typeSoin); }

  protected envoyer(): void {
    this.soin.demander({ ...this.demande, patientId: this.patientId }).subscribe({
      next: () => {
        this.succes('Demande envoyée à ' + nomPharmacie(this.demande.pharmacieId) + '. Vous serez informé dès qu\'un infirmier est affecté.');
        const { adresse, latitude, longitude } = this.demande;   // on garde la position pour la prochaine demande
        this.demande = { ...this.demandeVide(), adresse, latitude, longitude };
        this.charger();
      },
      error: e => this.afficherErreur(e)
    });
  }

  protected annuler(v: Visite): void {
    this.soin.annuler(v.id).subscribe({
      next: () => { this.succes('Demande annulée.'); this.charger(); },
      error: e => this.afficherErreur(e)
    });
  }

  private demandeVide(): VisiteRequest {
    return { patientId: '', pharmacieId: PHARMACIES[0].id, date: '', adresse: '', typeSoin: 'INJECTION', medicamentSensible: false };
  }

  private succes(m: string): void { this.erreur.set(''); this.message.set(m); }
  private afficherErreur(e: HttpErrorResponse): void { this.message.set(''); this.erreur.set(messageErreur(e)); }
}
