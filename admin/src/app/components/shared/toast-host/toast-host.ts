import { Component, inject } from '@angular/core';
import { NotifyService } from '../../../services/notify';

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="pointer-events-none fixed right-5 bottom-5 z-[1100] flex w-80 flex-col gap-2">
      @for (toast of notify.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg"
          [class]="toast.tone === 'success' ? 'border-support/30 bg-support-soft text-support' : 'border-danger/30 bg-danger-soft text-danger'"
          role="status"
        >
          <span class="flex-1">{{ toast.message }}</span>
          <button type="button" class="cursor-pointer opacity-60 hover:opacity-100" (click)="notify.dismiss(toast.id)" aria-label="Cerrar">✕</button>
        </div>
      }
    </div>
  `,
})
export class ToastHost {
  protected readonly notify = inject(NotifyService);
}
