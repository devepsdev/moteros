import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { etiquetaDificultad, etiquetaTerreno, formatDuracion, formatKm } from '../../../core/labels';
import { PageResponse, RutaSummary } from '../../../models/api.model';
import { toApiProblem } from '../../../services/api-error';
import { NotifyService } from '../../../services/notify';
import { RutaService } from '../../../services/ruta';
import { Pagination } from '../../shared/pagination/pagination';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-ruta-list',
  imports: [RouterLink, Pagination],
  templateUrl: './ruta-list.html',
})
export class RutaList {
  private rutas = inject(RutaService);
  private notify = inject(NotifyService);

  protected readonly etiquetaDificultad = etiquetaDificultad;
  protected readonly etiquetaTerreno = etiquetaTerreno;
  protected readonly formatKm = formatKm;
  protected readonly formatDuracion = formatDuracion;

  protected readonly result = signal<PageResponse<RutaSummary> | null>(null);
  protected readonly loading = signal(true);
  private texto = '';
  private page = 0;
  private readonly busqueda = new Subject<string>();

  constructor() {
    // Espera a que se deje de escribir antes de buscar.
    this.busqueda.pipe(debounceTime(300), distinctUntilChanged()).subscribe((texto) => {
      this.texto = texto;
      this.page = 0;
      this.load();
    });
    this.load();
  }

  protected buscar(texto: string): void {
    this.busqueda.next(texto.trim());
  }

  protected goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.rutas.list(this.texto, this.page, PAGE_SIZE).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (cause: unknown) => {
        this.notify.error(toApiProblem(cause).message);
        this.loading.set(false);
      },
    });
  }
}
