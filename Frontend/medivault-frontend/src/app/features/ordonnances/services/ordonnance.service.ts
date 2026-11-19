import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ordonnance, OrdonnanceRequest } from '../models/ordonnance.model';

@Injectable({ providedIn: 'root' })
export class OrdonnanceService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/ordonnances';

  parPatient(patientId: number): Observable<Ordonnance[]> {
    return this.http.get<Ordonnance[]>(`${this.url}/patient/${patientId}`);
  }

  toutes(): Observable<Ordonnance[]> {
    return this.http.get<Ordonnance[]>(this.url);
  }

  rechercherPatients(query: string): Observable<number[]> {
    return this.http.get<number[]>(`${this.url}/patients/search`, {
      params: { q: query }
    });
  }

  creer(request: OrdonnanceRequest): Observable<Ordonnance> {
    return this.http.post<Ordonnance>(this.url, request);
  }

  modifier(id: number, request: OrdonnanceRequest): Observable<Ordonnance> {
    return this.http.put<Ordonnance>(`${this.url}/${id}`, request);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  modifierStatut(id: number, statut: string): Observable<Ordonnance> {
    return this.http.patch<Ordonnance>(
      `${this.url}/${id}/statut?statut=${encodeURIComponent(statut)}`,
      null
    );
  }
}
