import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth';

/**
 * Añade el token y, si la API lo rechaza, lo renueva una vez y repite la petición.
 * El backend de moter@s responde 403 (no 401) con el token caducado, así que se reintenta
 * ante ambos. Si la renovación falla, la sesión se da por terminada.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isAuthEndpoint = req.url.startsWith('/api/auth/');
  const token = auth.accessToken();

  const withToken = (value: string) => req.clone({ setHeaders: { Authorization: `Bearer ${value}` } });

  return next(token && !isAuthEndpoint ? withToken(token) : req).pipe(
    catchError((error: unknown) => {
      const rechazado = error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
      if (!rechazado || isAuthEndpoint || !token) {
        return throwError(() => error);
      }
      return auth.refreshAccessToken().pipe(
        // Este catchError va antes del switchMap a propósito: solo debe cerrar la sesión
        // si falla la renovación, no si falla la petición repetida (un 403 real por permisos).
        catchError(() => {
          auth.expireSession();
          return throwError(() => error);
        }),
        switchMap((fresh) => next(withToken(fresh))),
      );
    }),
  );
};
