import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, tap, throwError, timeout } from 'rxjs';
import {
  AccesDossierResponse,
  AllergiesResponse,
  Consultation,
  ConsultationUpdateRequest,
  Dossier,
  FiltresConsultation,
  NouvelleConsultation,
  PageResponse,
  ProblemDetails
} from '../models/dossier.model';

// Service gérant toutes les interactions avec le microservice dossier-service (port 8085)
@Injectable({ providedIn: 'root' })
export class DossierService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/dossiers';

  /** true si l'API répond normalement, false si injoignable (0, 500 proxy error ou 503) */
  readonly enLigne = signal<boolean | null>(null);

  /**
   * Récupère le dossier médical complet d'un patient par son identifiant
   * GET /dossiers/patient/{patientId}
   */
  getDossier(patientId: string): Observable<Dossier> {
    return this.http.get<Dossier>(`${this.baseUrl}/patient/${encodeURIComponent(patientId)}`).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 0 || err.status === 503 || err.status === 500) {
          this.enLigne.set(false);
        } else {
          this.enLigne.set(true);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Récupère uniquement les allergies d'un patient
   * GET /dossiers/patient/{patientId}/allergies
   */
  getAllergies(patientId: string): Observable<AllergiesResponse> {
    return this.http.get<AllergiesResponse>(`${this.baseUrl}/patient/${encodeURIComponent(patientId)}/allergies`).pipe(
      timeout(4000)
    );
  }

  /**
   * Ajoute une nouvelle consultation médicale à un dossier
   * POST /dossiers/{id}/consultations
   */
  ajouterConsultation(dossierId: number, consultation: NouvelleConsultation): Observable<Consultation> {
    return this.http.post<Consultation>(`${this.baseUrl}/${dossierId}/consultations`, consultation);
  }

  /**
   * Modifie une consultation existante dans un dossier
   * PUT /dossiers/{id}/consultations/{consultationId}
   */
  modifierConsultation(
    dossierId: number,
    consultationId: number,
    body: ConsultationUpdateRequest
  ): Observable<Consultation> {
    return this.http.put<Consultation>(
      `${this.baseUrl}/${dossierId}/consultations/${consultationId}`,
      body
    );
  }

  /**
   * Supprime une consultation d'un dossier
   * DELETE /dossiers/{id}/consultations/{consultationId}?medecinId={medecinId}
   */
  supprimerConsultation(
    dossierId: number,
    consultationId: number,
    medecinId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${dossierId}/consultations/${consultationId}`,
      { params: { medecinId } }
    );
  }

  /**
   * Liste paginée des consultations d'un dossier avec filtres optionnels
   * GET /dossiers/{id}/consultations?page=&size=&medecinId=&du=&au=
   */
  listerConsultations(
    dossierId: number,
    opts: { page?: number; size?: number } & FiltresConsultation
  ): Observable<PageResponse<Consultation>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 10));
    if (opts.medecinId) params = params.set('medecinId', opts.medecinId);
    if (opts.du)        params = params.set('du', opts.du);
    if (opts.au)        params = params.set('au', opts.au);
    return this.http.get<PageResponse<Consultation>>(`${this.baseUrl}/${dossierId}/consultations`, { params });
  }

  /**
   * Télécharge les consultations d'un patient au format PDF
   * GET /dossiers/patient/{patientId}/consultations/export
   */
  telechargerPdf(patientId: string, opts: FiltresConsultation = {}): Observable<Blob> {
    let params = new HttpParams();
    if (opts.medecinId) params = params.set('medecinId', opts.medecinId);
    if (opts.du)        params = params.set('du', opts.du);
    if (opts.au)        params = params.set('au', opts.au);
    return this.http.get(
      `${this.baseUrl}/patient/${encodeURIComponent(patientId)}/consultations/export`,
      { params, responseType: 'blob' }
    );
  }

  /**
   * Vérifie l'accès actif d'un professionnel à un dossier
   * GET /dossiers/patient/{patientId}/acces?professionnelId={professionnelId}
   */
  verifierAcces(patientId: string, professionnelId: string): Observable<AccesDossierResponse> {
    return this.http.get<AccesDossierResponse>(`${this.baseUrl}/patient/${encodeURIComponent(patientId)}/acces`, {
      params: { professionnelId }
    });
  }

  /**
   * Modifie (retire ou accorde) l'accès d'un professionnel au dossier
   * PATCH /dossiers/{id}/acces/{professionnelId}
   */
  modifierAcces(dossierId: number, professionnelId: string): Observable<AccesDossierResponse> {
    return this.http.patch<AccesDossierResponse>(
      `${this.baseUrl}/${dossierId}/acces/${encodeURIComponent(professionnelId)}`,
      {}
    );
  }

  /**
   * Décode une erreur HTTP en message lisible (Problem Details RFC 9457)
   */
  formaterErreur(err: unknown): { status: number; message: string; detail?: string } {
    if (err instanceof HttpErrorResponse) {
      const problem = (err.error && typeof err.error === 'object' ? err.error : {}) as ProblemDetails;
      const detail = problem.detail || (typeof err.error === 'string' ? err.error : undefined);

      switch (err.status) {
        case 404:
          return { status: 404, message: 'Ressource introuvable', detail: detail || 'Le dossier médical ou la consultation demandée n\'a pas été trouvé(e).' };
        case 403:
          return { status: 403, message: 'Accès refusé', detail: detail || 'Vous n\'avez pas l\'autorisation requise pour effectuer cette opération.' };
        case 503:
          return { status: 503, message: 'Service indisponible', detail: detail || 'Le microservice dossier-service (port 8085) est actuellement indisponible.' };
        case 500:
          return { status: 500, message: problem.title || 'Erreur serveur (500)', detail: detail || 'Le microservice dossier-service est inaccessible ou a rencontré une erreur interne.' };
        case 400:
          return { status: 400, message: 'Données invalides', detail: detail || 'Certains champs de la requête sont invalides ou manquants.' };
        case 409:
          return { status: 409, message: 'Conflit métier', detail: detail || 'L\'opération viole une règle métier.' };
        case 0:
          return { status: 0, message: 'Connexion impossible', detail: 'Impossible de joindre le service dossier-service. Assurez-vous que le backend est démarré sur le port 8085.' };
        default:
          return { status: err.status, message: problem.title || `Erreur HTTP ${err.status}`, detail: detail || 'Une erreur inattendue est survenue.' };
      }
    }

    return { status: -1, message: 'Erreur inattendue', detail: (err as Error)?.message || 'Une erreur est survenue lors de l\'opération.' };
  }
}
