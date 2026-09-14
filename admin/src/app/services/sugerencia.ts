import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse, EstadoSugerencia, PageResponse, SugerenciaRuta } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class SugerenciaService {
  private http = inject(HttpClient);

  list(estado: EstadoSugerencia | null, page: number, size: number): Observable<PageResponse<SugerenciaRuta>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (estado) params = params.set('estado', estado);
    return this.http
      .get<ApiResponse<PageResponse<SugerenciaRuta>>>('/api/admin/sugerencias-ruta', { params })
      .pipe(map((res) => res.data));
  }

  get(uuid: string): Observable<SugerenciaRuta> {
    return this.http.get<ApiResponse<SugerenciaRuta>>(`/api/admin/sugerencias-ruta/${uuid}`).pipe(map((res) => res.data));
  }

  approve(uuid: string, rutaUuid: string): Observable<SugerenciaRuta> {
    return this.http
      .put<ApiResponse<SugerenciaRuta>>(`/api/admin/sugerencias-ruta/${uuid}/aprobar`, { rutaUuid })
      .pipe(map((res) => res.data));
  }

  reject(uuid: string, motivo: string): Observable<SugerenciaRuta> {
    return this.http
      .put<ApiResponse<SugerenciaRuta>>(`/api/admin/sugerencias-ruta/${uuid}/rechazar`, { motivo })
      .pipe(map((res) => res.data));
  }
}
