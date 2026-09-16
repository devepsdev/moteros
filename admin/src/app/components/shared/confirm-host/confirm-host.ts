import { Component, inject } from '@angular/core';
import { ConfirmService } from '../../../services/confirm';

@Component({
  selector: 'app-confirm-host',
  template: `
    @if (confirm.pending(); as request) {
      <div class="fixed inset-0 z-[1050] flex items-center justify-center bg-black/60 p-4" (click)="confirm.answer(false)">
        <div class="card w-full max-w-md p-6 shadow-xl" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <h2 class="display text-2xl">{{ request.title }}</h2>
          <p class="mt-2 whitespace-pre-line text-sm leading-relaxed text-ink-muted">{{ request.message }}</p>
          <div class="mt-6 flex justify-end gap-2">
            <button type="button" class="btn btn-secondary" (click)="confirm.answer(false)">Cancelar</button>
            <button type="button" class="btn" [class]="request.danger ? 'btn-danger' : 'btn-primary'" (click)="confirm.answer(true)">
              {{ request.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmHost {
  protected readonly confirm = inject(ConfirmService);
}
