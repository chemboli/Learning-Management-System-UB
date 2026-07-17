import { Injectable, signal } from '@angular/core';

interface ConfirmState {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  resolve?: (value: boolean) => void;
}

const INITIAL_STATE: ConfirmState = {
  visible: false,
  title: '',
  message: '',
  confirmLabel: 'Delete'
};

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  state = signal<ConfirmState>(INITIAL_STATE);

  ask(message: string, title = 'Are you sure?', confirmLabel = 'Delete'): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.state.set({ visible: true, title, message, confirmLabel, resolve });
    });
  }

  confirm() {
    this.state().resolve?.(true);
    this.state.set(INITIAL_STATE);
  }

  cancel() {
    this.state().resolve?.(false);
    this.state.set(INITIAL_STATE);
  }
}
