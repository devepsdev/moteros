import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiResponse, EstadisticasGlobales } from '../models/api.model';

/**
 * Contadores compartidos: los usa el resumen y también las insignias del menú lateral,
 * así que se guardan en un signal y cualquier pantalla que cambie algo relevante los refresca.
 */
@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);

  readonly stats = signal<EstadisticasGlobales | null>(null);

  refresh(): void {
    this.http.get<ApiResponse<EstadisticasGlobales>>('/api/admin/estadisticas').subscribe({
      next: (res) => this.stats.set(res.data),
      error: () => undefined,
    });
  }
}
