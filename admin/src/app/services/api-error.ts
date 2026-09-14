import { HttpErrorResponse } from '@angular/common/http';

export interface ApiProblem {
  status: number;
  message: string;
  /** Errores de validación por campo, tal como los devuelve GlobalExceptionHandler en `data`. */
  fieldErrors: Record<string, string>;
}

export function toApiProblem(error: unknown): ApiProblem {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return { status: 0, message: 'No hay conexión con el servidor.', fieldErrors: {} };
    }
    const body = error.error as { message?: string; data?: unknown } | null;
    const data = body?.data;
    const fieldErrors = data && typeof data === 'object' && !Array.isArray(data) ? (data as Record<string, string>) : {};
    const first = Object.values(fieldErrors)[0];
    // En los errores de validación el mensaje general es genérico: se muestra el del primer campo.
    const message =
      body?.message === 'Error de validacion' && first
        ? first
        : body?.message && !body.message.startsWith('Error interno')
          ? body.message
          : error.status === 403
            ? 'No tienes permiso para hacer esto.'
            : 'Ha ocurrido un error inesperado.';
    return { status: error.status, message, fieldErrors };
  }
  return {
    status: -1,
    message: error instanceof Error ? error.message : 'Ha ocurrido un error inesperado.',
    fieldErrors: {},
  };
}
