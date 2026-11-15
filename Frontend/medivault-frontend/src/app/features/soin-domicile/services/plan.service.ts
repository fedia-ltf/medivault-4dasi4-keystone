import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanSuivi, PlanSuiviRequest, Visite } from '../models/visite.model';

// Appels HTTP vers /api/plans du soin-domicile-service (espace médecin)
@Injectable({ providedIn: 'root' })
export class PlanService {
  private http = inject(HttpClient);
  private readonly url = '/api/plans';

  lister(medecinId: string): Observable<PlanSuivi[]> {
    return this.http.get<PlanSuivi[]>(this.url, { params: new HttpParams().set('medecinId', medecinId) });
  }

  creer(r: PlanSuiviRequest): Observable<PlanSuivi> {
    return this.http.post<PlanSuivi>(this.url, r);
  }

  visites(id: number): Observable<Visite[]> {
    return this.http.get<Visite[]>(`${this.url}/${id}/visites`);
  }

  arreter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
