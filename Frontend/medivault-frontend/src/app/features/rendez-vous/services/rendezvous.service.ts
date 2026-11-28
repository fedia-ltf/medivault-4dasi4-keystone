import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap, timeout } from 'rxjs';
import { Creneau, CreneauRequest, RendezVous, RendezVousRequest } from '../models/rendezvous.model';

// Données de démonstration si l'API ne répond pas
const DEMO_CRENEAUX: Creneau[] = [
  { id: 1, professionnelId: 'prof-001', typeProfessionnel: 'MEDECIN', dateDebut: new Date(Date.now() + 86400000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', dateFin: new Date(Date.now() + 86400000 + 1800000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', libre: true },
  { id: 2, professionnelId: 'prof-001', typeProfessionnel: 'MEDECIN', dateDebut: new Date(Date.now() + 90000000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', dateFin: new Date(Date.now() + 91800000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', libre: true },
  { id: 3, professionnelId: 'prof-002', typeProfessionnel: 'LABORANTIN', dateDebut: new Date(Date.now() + 172800000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', dateFin: new Date(Date.now() + 174600000).toISOString().replace('T', ' ').slice(0, 16).replace(' ', 'T') + ':00', libre: true },
];

const DEMO_RDV: RendezVous[] = [
  { id: 1, patientId: 'patient-001', motif: 'Consultation de routine', statut: 'CONFIRME', analyseId: null, creneauId: 1, professionnelId: 'prof-001', typeProfessionnel: 'MEDECIN', dateDebut: new Date(Date.now() + 86400000).toISOString(), dateFin: new Date(Date.now() + 88200000).toISOString() },
  { id: 2, patientId: 'patient-001', motif: 'Suivi cardiologie', statut: 'TERMINE', analyseId: null, creneauId: 2, professionnelId: 'prof-001', typeProfessionnel: 'MEDECIN', dateDebut: new Date(Date.now() - 172800000).toISOString(), dateFin: new Date(Date.now() - 171000000).toISOString() },
  { id: 3, patientId: 'patient-001', motif: 'Prise de sang', statut: 'ANNULE', analyseId: 5, creneauId: 3, professionnelId: 'prof-002', typeProfessionnel: 'LABORANTIN', dateDebut: new Date(Date.now() - 86400000).toISOString(), dateFin: new Date(Date.now() - 84600000).toISOString() },
];

@Injectable({ providedIn: 'root' })
export class RendezvousService {
  private http = inject(HttpClient);
  private readonly url = '/api/rendezvous';
  private readonly urlCreneaux = '/api/creneaux';

  /** true si l'API répond, false si on affiche les données de démonstration */
  readonly enLigne = signal<boolean | null>(null);
  private demoRdv = [...DEMO_RDV];
  private demoCreneaux = [...DEMO_CRENEAUX];

  // ─── Créneaux ───────────────────────────────────────────
  creneauxLibres(professionnelId: string, date?: string): Observable<Creneau[]> {
    let params: Record<string, string> = { professionnelId };
    if (date) params['date'] = date;
    return this.http.get<Creneau[]>(this.urlCreneaux, { params }).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const estLabo = professionnelId.toLowerCase().includes('labo');
        const correspondants = this.demoCreneaux.filter(c =>
          c.libre && (c.professionnelId === professionnelId || (estLabo ? c.typeProfessionnel === 'LABORANTIN' : c.typeProfessionnel === 'MEDECIN'))
        );
        return of(correspondants.length > 0 ? correspondants : this.demoCreneaux.filter(c => c.libre));
      })
    );
  }

  genererCreneaux(professionnelId: string): Observable<Creneau[]> {
    return this.http.post<Creneau[]>(`${this.urlCreneaux}/generer`, null, { params: { professionnelId } }).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const estLabo = professionnelId.toLowerCase().includes('labo');
        const type = estLabo ? 'LABORANTIN' : 'MEDECIN';
        const baseId = Math.max(0, ...this.demoCreneaux.map(c => c.id)) + 1;
        const nouveaux: Creneau[] = [
          { id: baseId, professionnelId, typeProfessionnel: type, dateDebut: new Date(Date.now() + 86400000).toISOString(), dateFin: new Date(Date.now() + 88200000).toISOString(), libre: true },
          { id: baseId + 1, professionnelId, typeProfessionnel: type, dateDebut: new Date(Date.now() + 90000000).toISOString(), dateFin: new Date(Date.now() + 91800000).toISOString(), libre: true },
          { id: baseId + 2, professionnelId, typeProfessionnel: type, dateDebut: new Date(Date.now() + 172800000).toISOString(), dateFin: new Date(Date.now() + 174600000).toISOString(), libre: true }
        ];
        this.demoCreneaux = [...this.demoCreneaux, ...nouveaux];
        return of(nouveaux);
      })
    );
  }

  creerCreneau(request: CreneauRequest): Observable<Creneau> {
    return this.http.post<Creneau>(this.urlCreneaux, request).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const nouveau: Creneau = {
          id: Math.max(0, ...this.demoCreneaux.map(c => c.id)) + 1,
          professionnelId: request.professionnelId || 'medecin-karim',
          typeProfessionnel: request.typeProfessionnel || 'MEDECIN',
          dateDebut: request.dateDebut,
          dateFin: request.dateFin,
          libre: true
        };
        this.demoCreneaux.push(nouveau);
        return of(nouveau);
      })
    );
  }

  modifierCreneau(id: number, request: CreneauRequest): Observable<Creneau> {
    return this.http.put<Creneau>(`${this.urlCreneaux}/${id}`, request).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const c = this.demoCreneaux.find(x => x.id === id);
        if (c) {
          c.dateDebut = request.dateDebut;
          c.dateFin = request.dateFin;
        }
        return of(c!);
      })
    );
  }

  supprimerCreneau(id: number): Observable<void> {
    return this.http.delete<void>(`${this.urlCreneaux}/${id}`).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        this.demoCreneaux = this.demoCreneaux.filter(c => c.id !== id);
        return of(void 0);
      })
    );
  }

  // ─── Rendez-vous ────────────────────────────────────────
  tousLesRendezVous(): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(this.url).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => { this.enLigne.set(false); return of([...this.demoRdv]); })
    );
  }

  parPatient(patientId: string): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(`${this.url}/patient/${patientId}`).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const filtres = this.demoRdv.filter(r => r.patientId === patientId);
        return of(filtres.length > 0 ? filtres : [...this.demoRdv]);
      })
    );
  }

  parProfessionnel(professionnelId: string): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(`${this.url}/professionnel/${professionnelId}`).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => { this.enLigne.set(false); return of([...this.demoRdv]); })
    );
  }

  reserver(request: RendezVousRequest): Observable<RendezVous> {
    return this.http.post<RendezVous>(this.url, request).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const creneau = this.demoCreneaux.find(c => c.id === Number(request.creneauId));
        if (creneau) creneau.libre = false;
        const dateDebut = creneau ? creneau.dateDebut : new Date(Date.now() + 86400000).toISOString();
        const dateFin = creneau ? creneau.dateFin : new Date(Date.now() + 88200000).toISOString();
        const demo: RendezVous = {
          id: Math.floor(Math.random() * 900) + 100,
          ...request,
          statut: 'CONFIRME',
          analyseId: request.analyseId ?? null,
          professionnelId: creneau ? creneau.professionnelId : 'medecin-karim',
          typeProfessionnel: creneau ? creneau.typeProfessionnel : 'MEDECIN',
          dateDebut,
          dateFin
        };
        this.demoRdv.push(demo);
        return of(demo);
      })
    );
  }

  annuler(id: number): Observable<RendezVous> {
    return this.http.patch<RendezVous>(`${this.url}/${id}`, null).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        const rdv = this.demoRdv.find(r => r.id === id);
        if (rdv) rdv.statut = 'ANNULE';
        return of(rdv!);
      })
    );
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => { this.demoRdv = this.demoRdv.filter(r => r.id !== id); return of(void 0); })
    );
  }
}
