import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  tone: 'success' | 'error';
}

@Injectable({ providedIn: 'root' })
export class NotifyService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  success(message: string): void {
    this.push(message, 'success');
  }

  error(message: string): void {
    this.push(message, 'error');
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((toast) => toast.id !== id));
  }

  private push(message: string, tone: Toast['tone']): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, message, tone }]);
    setTimeout(() => this.dismiss(id), tone === 'error' ? 6000 : 3500);
  }
}
