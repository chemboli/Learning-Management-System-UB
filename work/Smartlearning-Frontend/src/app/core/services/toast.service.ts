import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'danger' | 'info' | 'warning';

export interface ToastMessage {
  id: number;
  type: ToastType;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  toasts = signal<ToastMessage[]>([]);

  show(text: string, type: ToastType = 'info', durationMs = 4000) {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, type, text }]);

    setTimeout(() => this.dismiss(id), durationMs);
  }

  success(text: string) {
    this.show(text, 'success');
  }

  error(text: string) {
    this.show(text, 'danger', 5000);
  }

  info(text: string) {
    this.show(text, 'info');
  }

  dismiss(id: number) {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
