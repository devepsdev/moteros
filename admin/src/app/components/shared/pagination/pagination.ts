import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    @if (totalPages() > 1) {
      <nav class="flex items-center justify-between gap-4 border-t border-line px-5 py-3">
        <p class="text-xs text-ink-muted">Página {{ page() + 1 }} de {{ totalPages() }} · {{ totalElements() }} en total</p>
        <div class="flex gap-2">
          <button type="button" class="btn btn-secondary btn-sm" [disabled]="page() === 0" (click)="pageChange.emit(page() - 1)">Anterior</button>
          <button type="button" class="btn btn-secondary btn-sm" [disabled]="isLast()" (click)="pageChange.emit(page() + 1)">Siguiente</button>
        </div>
      </nav>
    }
  `,
})
export class Pagination {
  /** Página actual, empezando en 0 como en Spring. */
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly pageChange = output<number>();

  protected readonly isLast = computed(() => this.page() >= this.totalPages() - 1);
}
