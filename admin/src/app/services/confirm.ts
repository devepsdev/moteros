import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  danger?: boolean;
}

interface PendingConfirm extends ConfirmRequest {
  resolve: (confirmed: boolean) => void;
}

/** Diálogo de confirmación propio, en vez del confirm() nativo del navegador. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly pending = signal<PendingConfirm | null>(null);

  ask(request: ConfirmRequest): Promise<boolean> {
    return new Promise((resolve) => this.pending.set({ ...request, resolve }));
  }

  answer(confirmed: boolean): void {
    this.pending()?.resolve(confirmed);
    this.pending.set(null);
  }
}
