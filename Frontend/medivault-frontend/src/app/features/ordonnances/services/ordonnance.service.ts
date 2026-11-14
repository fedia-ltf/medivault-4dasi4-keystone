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

  creer(request: OrdonnanceRequest): Observable<Ordonnance> {
    return this.http.post<Ordonnance>(this.url, request);
  }
}
