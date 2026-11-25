import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { SoinService, messageErreur } from '../services/soin.service';
import { InfirmierService } from '../services/infirmier.service';
import {
  Infirmier, InfirmierRequest, PHARMACIE_DU_PHARMACIEN, lienCarte, RapportRequest, RechercheVisites, STATUTS, TYPES_SOIN,
  Visite, libelleSoin, nomPharmacie
} from '../models/visite.model';

type Onglet = 'attente' | 'arealiser' | 'historique' | 'infirmiers';

// Espace pharmacien : gérer ses infirmiers, confirmer les demandes, affecter un infirmier disponible, saisir le rapport
@Component({
  selector: 'mv-espace-pharmacien',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, Icon],
  templateUrl: './espace-pharmacien.html'
})
export class EspacePharmacien implements OnInit {
  private soin = inject(SoinService);
  private auth = inject(AuthService);
  private infirmierApi = inject(InfirmierService);
  private destroyRef = inject(DestroyRef);

  protected readonly statuts = STATUTS;
  protected readonly infirmiers = signal<Infirmier[]>([]);
  protected readonly libelleSoin = libelleSoin;
  protected readonly lienCarte = lienCarte;
  protected readonly pharmacie = nomPharmacie(PHARMACIE_DU_PHARMACIEN);

  protected readonly visites = signal<Visite[]>([]);
  protected readonly onglet = signal<Onglet>('attente');
  protected readonly message = signal('');
  protected readonly erreur = signal('');
  protected readonly enAffectation = signal<Visite | null>(null);
  protected readonly enRapport = signal<Visite | null>(null);

  protected readonly enAttente = computed(() => this.trier(this.visites().filter(v => v.statut === 'DEMANDEE')));
  protected readonly aRealiser = computed(() => this.trier(this.visites().filter(v => v.statut === 'ACCEPTEE')));
  protected readonly historique = computed(() =>
    this.trier(this.visites().filter(v => v.statut === 'REALISEE' || v.statut === 'ANNULEE')).reverse());

  // ---------- Recherche (côté serveur : /api/visites/recherche et /api/infirmiers/recherche) ----------
  protected readonly typesSoin = TYPES_SOIN;
  protected filtre: RechercheVisites = this.filtreVide();
  protected rechercheInfirmier = '';
  /** null = pas de recherche en cours : on affiche toutes les visites / tous les infirmiers. */
  protected readonly resultats = signal<Visite[] | null>(null);
  protected readonly infirmiersTrouves = signal<Infirmier[] | null>(null);
  private readonly rechercheVisites$ = new Subject<RechercheVisites>();
  private readonly rechercheInfirmiers$ = new Subject<string>();

  // Les onglets montrent le résultat de la recherche ; les compteurs du haut restent sur toutes les visites
  private readonly source = computed(() => this.resultats() ?? this.visites());
  protected readonly fAttente = computed(() => this.trier(this.source().filter(v => v.statut === 'DEMANDEE')));
  protected readonly fARealiser = computed(() => this.trier(this.source().filter(v => v.statut === 'ACCEPTEE')));
  protected readonly fHistorique = computed(() =>
    this.trier(this.source().filter(v => v.statut === 'REALISEE' || v.statut === 'ANNULEE')).reverse());
  protected readonly liste = computed(() =>
    this.onglet() === 'attente' ? this.fAttente() : this.onglet() === 'arealiser' ? this.fARealiser() : this.fHistorique());
  protected readonly infirmiersAffiches = computed(() => this.infirmiersTrouves() ?? this.infirmiers());

  protected infirmierChoisi: Infirmier | null = null;
  protected readonly enEdition = signal<number | 'nouveau' | null>(null);
  protected fiche: InfirmierRequest = this.ficheVide();
  protected rapport: RapportRequest = { pharmacienId: '' };

  private get pharmacienId(): string { return this.auth.utilisateur()?.email ?? ''; }

  ngOnInit(): void {
    // On attend 300 ms après la dernière frappe, et seule la dernière recherche compte (switchMap)
    this.rechercheVisites$.pipe(
      debounceTime(300),
      switchMap(f => this.soin.rechercher(f).pipe(catchError(e => { this.afficherErreur(e); return EMPTY; }))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(v => this.resultats.set(v));

    this.rechercheInfirmiers$.pipe(
      debounceTime(300),
      switchMap(q => this.infirmierApi.rechercher(PHARMACIE_DU_PHARMACIEN, q)
        .pipe(catchError(e => { this.afficherErreur(e); return EMPTY; }))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(l => this.infirmiersTrouves.set(l));

    this.charger();
  }

  protected charger(): void {
    this.soin.lister({ pharmacieId: PHARMACIE_DU_PHARMACIEN }).subscribe({
      next: v => this.visites.set(v),
      error: e => this.afficherErreur(e)
    });
    this.chargerInfirmiers();
    this.rechercher();             // garde le résultat de la recherche à jour après une action
    this.rechercherInfirmiers();
  }

  /** Lance la recherche des visites, ou l'annule si aucun critère n'est rempli. */
  protected rechercher(): void {
    const { q, typeSoin, du, au } = this.filtre;
    if (!q?.trim() && !typeSoin && !du && !au) { this.resultats.set(null); return; }
    this.rechercheVisites$.next({ ...this.filtre, pharmacieId: PHARMACIE_DU_PHARMACIEN });
  }

  protected effacerRecherche(): void {
    this.filtre = this.filtreVide();
    this.resultats.set(null);
  }

  protected rechercherInfirmiers(): void {
    const q = this.rechercheInfirmier.trim();
    if (!q) { this.infirmiersTrouves.set(null); return; }
    this.rechercheInfirmiers$.next(q);
  }

  protected effacerRechercheInfirmiers(): void {
    this.rechercheInfirmier = '';
    this.infirmiersTrouves.set(null);
  }

  private filtreVide(): RechercheVisites { return { q: '', typeSoin: '', du: '', au: '' }; }

  // ---------- Mes infirmiers : ajouter, modifier, supprimer ----------
  private chargerInfirmiers(): void {
    this.infirmierApi.lister(PHARMACIE_DU_PHARMACIEN).subscribe({
      next: l => this.infirmiers.set(l),
      error: e => this.afficherErreur(e)
    });
  }

  protected nouvelInfirmier(): void {
    this.fiche = this.ficheVide();
    this.enEdition.set('nouveau');
  }

  protected modifierInfirmier(i: Infirmier): void {
    this.fiche = {
      prenom: i.prenom, nom: i.nom, telephone: i.telephone ?? '',
      heureDebut: i.heureDebut.slice(0, 5), heureFin: i.heureFin.slice(0, 5), pharmacieId: i.pharmacieId
    };
    this.enEdition.set(i.id);
  }

  protected enregistrerInfirmier(): void {
    const id = this.enEdition();
    const appel = id === 'nouveau' || id === null
      ? this.infirmierApi.ajouter(this.fiche)
      : this.infirmierApi.modifier(id, this.fiche);
    appel.subscribe({
      next: i => {
        this.enEdition.set(null);
        this.succes(id === 'nouveau' ? `${i.nomComplet} a été ajouté(e).` : `${i.nomComplet} a été modifié(e).`);
        this.charger();
      },
      error: e => this.afficherErreur(e)
    });
  }

  protected supprimerInfirmier(i: Infirmier): void {
    if (!confirm(`Supprimer ${i.nomComplet} de la pharmacie ?`)) return;
    this.infirmierApi.supprimer(i.id).subscribe({
      next: () => { this.succes(`${i.nomComplet} a été supprimé(e).`); this.chargerInfirmiers(); },
      error: e => this.afficherErreur(e)
    });
  }

  /** Nombre de soins confirmés à venir pour un infirmier. */
  protected soinsAVenir(i: Infirmier): number {
    return this.aRealiser().filter(v => v.infirmierNom === i.nomComplet).length;
  }

  private ficheVide(): InfirmierRequest {
    return { prenom: '', nom: '', telephone: '', heureDebut: '08:00', heureFin: '16:00', pharmacieId: PHARMACIE_DU_PHARMACIEN };
  }

  // 1. Confirmer la demande et affecter un infirmier
  protected ouvrirAffectation(v: Visite): void {
    this.enRapport.set(null);
    this.infirmierChoisi = null;
    this.enAffectation.set(v);
  }

  /**
   * RG9 : pourquoi l'infirmier n'est pas disponible pour cette visite ('' = disponible).
   * 1) la visite doit tomber dans ses horaires ; 2) pas d'autre soin confirmé à moins d'une heure.
   */
  protected indisponibilite(i: Infirmier, v: Visite): string {
    const heure = v.date.slice(11, 16);                  // "2026-11-18T10:30:00" -> "10:30"
    if (heure < i.heureDebut.slice(0, 5) || heure > i.heureFin.slice(0, 5)) {
      return `Hors horaires (${this.horaires(i)})`;
    }
    const t = Date.parse(v.date);
    const occupe = this.visites().some(x =>
      x.id !== v.id && x.statut === 'ACCEPTEE' && x.infirmierNom === i.nomComplet && Math.abs(Date.parse(x.date) - t) < 3_600_000);
    return occupe ? 'Déjà en soin à cette heure' : '';
  }

  protected horaires(i: Infirmier): string {
    return `${i.heureDebut.slice(0, 5)} – ${i.heureFin.slice(0, 5)}`;
  }

  protected choisirInfirmier(i: Infirmier): void {
    this.infirmierChoisi = i;
  }

  protected confirmer(): void {
    const v = this.enAffectation();
    const i = this.infirmierChoisi;
    if (!v || !i) return;
    // L'onglet WhatsApp est ouvert pendant le clic (sinon le navigateur bloque la popup), puis rempli après la réponse
    const fenetre = i.telephone ? window.open('', '_blank') : null;
    this.soin.accepter(v.id, { pharmacienId: this.pharmacienId, infirmierId: i.id }).subscribe({
      next: r => {
        this.enAffectation.set(null);
        if (r.whatsappEnvoye) {
          fenetre?.close();                       // déjà envoyé automatiquement par le backend
          this.succes(`Soin confirmé : ${i.nomComplet} se rend chez ${v.patientId}. Message WhatsApp envoyé à ${i.nomComplet}.`);
        } else if (fenetre) {
          this.ouvrirDansWhatsApp(r.id, fenetre); // WhatsApp s'ouvre avec le message prêt : il reste à cliquer sur Envoyer
          this.succes(`Soin confirmé : ${i.nomComplet} se rend chez ${v.patientId}. WhatsApp est ouvert avec le message pour ${i.nomComplet} : cliquez sur Envoyer.`);
        } else {
          this.succes(`Soin confirmé : ${i.nomComplet} se rend chez ${v.patientId}. ${i.nomComplet} n'a pas de numéro : pas de message WhatsApp.`);
        }
        this.charger();
      },
      error: e => { fenetre?.close(); this.afficherErreur(e); }
    });
  }

  /** Ouvre WhatsApp vers l'infirmier affecté, avec les infos du patient et du soin déjà écrites. */
  protected envoyerWhatsApp(v: Visite): void {
    this.ouvrirDansWhatsApp(v.id, window.open('', '_blank'));   // ouverte pendant le clic : sinon la popup est bloquée
  }

  private ouvrirDansWhatsApp(visiteId: number, fenetre: Window | null): void {
    this.soin.whatsapp(visiteId).subscribe({
      next: m => { if (fenetre) fenetre.location.href = m.lien; else window.location.href = m.lien; },
      error: e => { fenetre?.close(); this.afficherErreur(e); }
    });
  }

  // 2. Saisir le rapport de l'infirmier : il part automatiquement au dossier médical
  protected ouvrirRapport(v: Visite): void {
    this.enAffectation.set(null);
    this.rapport = { pharmacienId: this.pharmacienId, dose: v.dose, injectionFaite: v.typeSoin === 'INJECTION' };
    this.enRapport.set(v);
  }

  protected enregistrerRapport(): void {
    const v = this.enRapport();
    if (!v) return;
    this.soin.saisirRapport(v.id, this.rapport).subscribe({
      next: () => {
        this.enRapport.set(null);
        this.succes(`Rapport enregistré et ajouté au dossier médical de ${v.patientId}.`);
        this.charger();
      },
      error: e => this.afficherErreur(e)
    });
  }

  protected refuser(v: Visite): void {
    this.soin.annuler(v.id).subscribe({
      next: () => { this.succes('Demande refusée.'); this.charger(); },
      error: e => this.afficherErreur(e)
    });
  }

  private trier(l: Visite[]): Visite[] { return [...l].sort((a, b) => a.date.localeCompare(b.date)); }
  private succes(m: string): void { this.erreur.set(''); this.message.set(m); }
  private afficherErreur(e: HttpErrorResponse): void { this.message.set(''); this.erreur.set(messageErreur(e)); }
}
