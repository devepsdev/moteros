import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ROLES } from '../../../core/labels';
import { PageResponse, RolUsuario, UsuarioResponse } from '../../../models/api.model';
import { toApiProblem } from '../../../services/api-error';
import { AuthService } from '../../../services/auth';
import { ConfirmService } from '../../../services/confirm';
import { NotifyService } from '../../../services/notify';
import { StatsService } from '../../../services/stats';
import { UsuarioService } from '../../../services/usuario';
import { Pagination } from '../../shared/pagination/pagination';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-usuario-list',
  imports: [DatePipe, Pagination],
  templateUrl: './usuario-list.html',
})
export class UsuarioList {
  private usuarios = inject(UsuarioService);
  private notify = inject(NotifyService);
  private confirm = inject(ConfirmService);
  private stats = inject(StatsService);
  protected readonly auth = inject(AuthService);

  protected readonly roles = ROLES;
  protected readonly result = signal<PageResponse<UsuarioResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal<string | null>(null);
  private texto = '';
  private page = 0;
  private readonly busqueda = new Subject<string>();

  constructor() {
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

  protected async changeRole(usuario: UsuarioResponse, select: HTMLSelectElement): Promise<void> {
    const rol = select.value as RolUsuario;
    const label = ROLES.find((r) => r.value === rol)?.label.toLowerCase();
    const esYo = usuario.uuid === this.auth.user()?.uuid;
    const ok = await this.confirm.ask({
      title: 'Cambiar rol',
      message: esYo && rol !== 'admin'
        ? 'Vas a quitarte el rol de administrador: perderás el acceso a este panel.'
        : `${usuario.nombreCompleto} pasará a ser ${label}.`,
      confirmLabel: 'Cambiar rol',
      danger: esYo && rol !== 'admin',
    });
    if (!ok) {
      select.value = usuario.rol;
      return;
    }
    this.busy.set(usuario.uuid);
    this.usuarios.updateRole(usuario.uuid, rol).subscribe({
      next: (actualizado) => {
        this.notify.success(`${actualizado.nombreCompleto} ahora es ${label}.`);
        this.busy.set(null);
        if (esYo && rol !== 'admin') {
          this.auth.logout();
          return;
        }
        this.replace(actualizado);
        this.stats.refresh();
      },
      error: (cause: unknown) => {
        select.value = usuario.rol;
        this.notify.error(toApiProblem(cause).message);
        this.busy.set(null);
      },
    });
  }

  protected async toggleActive(usuario: UsuarioResponse): Promise<void> {
    const activar = !usuario.activo;
    const ok = await this.confirm.ask({
      title: activar ? 'Reactivar usuario' : 'Dar de baja',
      message: activar
        ? `${usuario.nombreCompleto} podrá volver a entrar en la app.`
        : `${usuario.nombreCompleto} no podrá entrar en la app y se cerrarán sus sesiones. Su contenido no se borra.`,
      confirmLabel: activar ? 'Reactivar' : 'Dar de baja',
      danger: !activar,
    });
    if (!ok) return;
    this.busy.set(usuario.uuid);
    this.usuarios.setActive(usuario.uuid, activar).subscribe({
      next: (actualizado) => {
        this.notify.success(activar ? `${actualizado.nombreCompleto} reactivado.` : `${actualizado.nombreCompleto} dado de baja.`);
        this.replace(actualizado);
        this.busy.set(null);
        this.stats.refresh();
      },
      error: (cause: unknown) => {
        this.notify.error(toApiProblem(cause).message);
        this.busy.set(null);
      },
    });
  }

  private replace(actualizado: UsuarioResponse): void {
    this.result.update((page) =>
      page ? { ...page, content: page.content.map((u) => (u.uuid === actualizado.uuid ? { ...u, ...actualizado } : u)) } : page,
    );
  }

  private load(): void {
    this.loading.set(true);
    this.usuarios.list(this.texto, this.page, PAGE_SIZE).subscribe({
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
