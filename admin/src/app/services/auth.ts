import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { ApiResponse, LoginResponse, UsuarioResponse } from '../models/api.model';

const ACCESS_KEY = 'moteros.admin.accessToken';
const REFRESH_KEY = 'moteros.admin.refreshToken';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly user = signal<UsuarioResponse | null>(null);
  readonly isAdmin = computed(() => this.user()?.rol === 'admin');

  private refreshInFlight: Observable<string> | null = null;

  accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  /** Acepta email o nombre de usuario, igual que la app. */
  login(identificador: string, password: string): Observable<UsuarioResponse> {
    return this.http.post<ApiResponse<LoginResponse>>('/api/auth/login', { identificador, password }).pipe(
      map((res) => res.data),
      switchMap((auth) => {
        this.storeTokens(auth);
        // El login lo acepta cualquier cuenta; el panel solo es para administradores.
        if (auth.usuario.rol !== 'admin') {
          this.clearSession(true);
          return throwError(() => new Error('Esta cuenta no tiene permisos de administración.'));
        }
        this.user.set(auth.usuario);
        return of(auth.usuario);
      }),
    );
  }

  /** Al arrancar: si hay tokens guardados, comprueba que siguen valiendo y que la cuenta sigue siendo admin. */
  restoreSession(): Promise<void> {
    if (!this.accessToken()) {
      return Promise.resolve();
    }
    return firstValueFrom(
      this.http.get<ApiResponse<UsuarioResponse>>('/api/usuarios/me').pipe(
        map((res) => res.data),
        tap((user) => (user.rol === 'admin' ? this.user.set(user) : this.clearSession(true))),
        map(() => undefined),
        catchError(() => {
          this.clearSession(false);
          return of(undefined);
        }),
      ),
    );
  }

  /**
   * Renueva el access token. Varias peticiones que fallan a la vez comparten la misma
   * renovación: el backend rota el refresh token en cada uso y una segunda llamada en
   * paralelo lo encontraría ya revocado.
   */
  refreshAccessToken(): Observable<string> {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (!refreshToken) {
      return throwError(() => new Error('No hay sesión que renovar.'));
    }
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http.post<ApiResponse<LoginResponse>>('/api/auth/refresh', { refreshToken }).pipe(
        map((res) => res.data),
        tap((auth) => this.storeTokens(auth)),
        map((auth) => auth.token),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay(1),
      );
    }
    return this.refreshInFlight;
  }

  logout(): void {
    this.clearSession(true);
    this.router.navigate(['/login']);
  }

  /** La sesión ya no se puede renovar: vuelta al login avisando del motivo. */
  expireSession(): void {
    this.clearSession(false);
    if (this.router.navigated) {
      this.router.navigate(['/login'], { queryParams: { caducada: 1 } });
    }
  }

  private storeTokens(auth: LoginResponse): void {
    localStorage.setItem(ACCESS_KEY, auth.token);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  }

  private clearSession(revokeOnServer: boolean): void {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.user.set(null);
    if (revokeOnServer && refreshToken) {
      // Sin esperar: la sesión local ya está cerrada aunque esto falle.
      this.http.post('/api/auth/logout', { refreshToken }).subscribe({ error: () => undefined });
    }
  }
}
