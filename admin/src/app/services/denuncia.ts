import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse, Denuncia, EstadoDenuncia, PageResponse } from '../models/api.model';

export interface ResolverDenuncia {
  eliminarContenido: boolean;
  darDeBaja: boolean;
  nota: string;
}

@Injectable({ providedIn: 'root' })
export class DenunciaService {
  private http = inject(HttpClient);

  list(estado: EstadoDenuncia | null, page: number, size: number): Observable<PageResponse<Denuncia>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (estado) params = params.set('estado', estado);
    return this.http.get<ApiResponse<PageResponse<Denuncia>>>('/api/admin/denuncias', { params }).pipe(map((res) => res.data));
  }

  resolve(uuid: string, body: ResolverDenuncia): Observable<Denuncia> {
    return this.http.put<ApiResponse<Denuncia>>(`/api/admin/denuncias/${uuid}/resolver`, body).pipe(map((res) => res.data));
  }
}
