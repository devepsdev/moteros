import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ESTADO_DENUNCIA_LABELS, MOTIVO_DENUNCIA_LABELS, TIPO_DENUNCIA_LABELS } from '../../../core/labels';
import { Denuncia, EstadoDenuncia, PageResponse } from '../../../models/api.model';
import { toApiProblem } from '../../../services/api-error';
import { ConfirmService } from '../../../services/confirm';
import { DenunciaService } from '../../../services/denuncia';
import { NotifyService } from '../../../services/notify';
import { StatsService } from '../../../services/stats';
import { Pagination } from '../../shared/pagination/pagination';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-denuncia-list',
  imports: [DatePipe, Pagination],
  templateUrl: './denuncia-list.html',
})
export class DenunciaList {
  private denuncias = inject(DenunciaService);
  private notify = inject(NotifyService);
  private confirm = inject(ConfirmService);
  private stats = inject(StatsService);

  protected readonly tabs: { value: EstadoDenuncia | null; label: string }[] = [
    { value: 'pendiente', label: 'Pendientes' },
    { value: 'resuelta', label: 'Resueltas' },
    { value: 'descartada', label: 'Descartadas' },
    { value: null, label: 'Todas' },
  ];
  protected readonly tipoLabels = TIPO_DENUNCIA_LABELS;
  protected readonly motivoLabels = MOTIVO_DENUNCIA_LABELS;
  protected readonly estadoLabels = ESTADO_DENUNCIA_LABELS;

  protected readonly estado = signal<EstadoDenuncia | null>('pendiente');
  protected readonly result = signal<PageResponse<Denuncia> | null>(null);
  protected readonly loading = signal(true);
  protected readonly selected = signal<Denuncia | null>(null);
  protected readonly eliminarContenido = signal(false);
  protected readonly darDeBaja = signal(false);
  protected readonly nota = signal('');
  protected readonly busy = signal(false);
  private page = 0;

  constructor() {
    this.load();
  }

  protected setEstado(estado: EstadoDenuncia | null): void {
    this.estado.set(estado);
    this.page = 0;
    this.selected.set(null);
    this.load();
  }

  protected goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  protected select(denuncia: Denuncia): void {
    this.selected.set(denuncia);
    this.eliminarContenido.set(false);
    this.darDeBaja.set(false);
    this.nota.set('');
  }

  /** Texto de la acción de borrar según lo denunciado: un perfil no se borra, se limpia. */
  protected etiquetaEliminar(d: Denuncia): string {
    return d.tipo === 'usuario' ? 'Quitar la foto y la biografía del perfil' : `Eliminar ${this.tipoLabels[d.tipo].toLowerCase()}`;
  }

  protected async resolve(): Promise<void> {
    const d = this.selected();
    if (!d) return;
    const medidas = this.eliminarContenido() || this.darDeBaja();
    const ok = await this.confirm.ask(
      medidas
        ? {
            title: 'Aplicar medidas',
            message: [
              this.eliminarContenido() ? `· ${this.etiquetaEliminar(d)}.` : '',
              this.darDeBaja() ? `· Dar de baja a @${d.denunciado?.nombreUsuario}: no podrá volver a entrar.` : '',
              'Se cerrarán también las demás denuncias pendientes sobre este contenido.',
            ]
              .filter(Boolean)
              .join('\n'),
            confirmLabel: 'Aplicar',
            danger: true,
          }
        : {
            title: 'Descartar denuncia',
            message: 'No se toma ninguna medida. Se cerrarán también las demás denuncias pendientes sobre este contenido.',
            confirmLabel: 'Descartar',
          },
    );
    if (!ok) return;

    this.busy.set(true);
    this.denuncias.resolve(d.uuid, { eliminarContenido: this.eliminarContenido(), darDeBaja: this.darDeBaja(), nota: this.nota().trim() }).subscribe({
      next: (res) => {
        this.notify.success(res.estado === 'resuelta' ? 'Medidas aplicadas.' : 'Denuncia descartada.');
        this.busy.set(false);
        this.selected.set(null);
        this.stats.refresh();
        this.load();
      },
      error: (cause: unknown) => {
        this.notify.error(toApiProblem(cause).message);
        this.busy.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.denuncias.list(this.estado(), this.page, PAGE_SIZE).subscribe({
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
