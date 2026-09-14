import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse, PageResponse, RolUsuario, UsuarioResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);

  /** Usuarios de la plataforma, los más recientes primero. Busca por nombre, usuario o email. */
  list(texto: string, page: number, size: number): Observable<PageResponse<UsuarioResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (texto) params = params.set('texto', texto);
    return this.http.get<ApiResponse<PageResponse<UsuarioResponse>>>('/api/admin/usuarios', { params }).pipe(map((res) => res.data));
  }

  updateRole(uuid: string, rol: RolUsuario): Observable<UsuarioResponse> {
    const params = new HttpParams().set('rol', rol);
    return this.http.patch<ApiResponse<UsuarioResponse>>(`/api/admin/usuarios/${uuid}/rol`, null, { params }).pipe(map((res) => res.data));
  }

  setActive(uuid: string, activo: boolean): Observable<UsuarioResponse> {
    const params = new HttpParams().set('activo', activo);
    return this.http.patch<ApiResponse<UsuarioResponse>>(`/api/admin/usuarios/${uuid}/activo`, null, { params }).pipe(map((res) => res.data));
  }
}
