import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse, PageResponse, RutaRequest, RutaResponse, RutaSummary } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class RutaService {
  private http = inject(HttpClient);

  list(texto: string, page: number, size: number): Observable<PageResponse<RutaSummary>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sortBy', 'fechaCreacion').set('sortDir', 'desc');
    if (texto) params = params.set('searchText', texto);
    return this.http.get<ApiResponse<PageResponse<RutaSummary>>>('/api/rutas', { params }).pipe(map((res) => res.data));
  }

  get(uuid: string): Observable<RutaResponse> {
    return this.http.get<ApiResponse<RutaResponse>>(`/api/rutas/${uuid}`).pipe(map((res) => res.data));
  }

  create(request: RutaRequest): Observable<RutaResponse> {
    return this.http.post<ApiResponse<RutaResponse>>('/api/rutas', request).pipe(map((res) => res.data));
  }

  update(uuid: string, request: RutaRequest): Observable<RutaResponse> {
    return this.http.put<ApiResponse<RutaResponse>>(`/api/rutas/${uuid}`, request).pipe(map((res) => res.data));
  }

  remove(uuid: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`/api/rutas/${uuid}`).pipe(map(() => undefined));
  }
}
