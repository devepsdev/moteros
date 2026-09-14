import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { ConfirmHost } from '../../components/shared/confirm-host/confirm-host';
import { ToastHost } from '../../components/shared/toast-host/toast-host';
import { AuthService } from '../../services/auth';
import { StatsService } from '../../services/stats';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastHost, ConfirmHost],
  templateUrl: './admin-layout.html',
})
export class AdminLayout {
  protected readonly auth = inject(AuthService);
  protected readonly statsService = inject(StatsService);
  protected readonly menuOpen = signal(false);

  constructor() {
    const router = inject(Router);
    this.statsService.refresh();
    // Las insignias (pendientes de revisar) se actualizan al cambiar de pantalla.
    router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      this.menuOpen.set(false);
      this.statsService.refresh();
    });
  }
}
