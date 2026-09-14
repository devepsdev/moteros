import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { StatsService } from '../../services/stats';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected readonly auth = inject(AuthService);
  protected readonly stats = inject(StatsService).stats;

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    const name = this.auth.user()?.nombreCompleto.split(' ')[0] ?? '';
    const salute = hour < 14 ? 'Buenos días' : hour < 21 ? 'Buenas tardes' : 'Buenas noches';
    return name ? `${salute}, ${name}` : salute;
  });
}
