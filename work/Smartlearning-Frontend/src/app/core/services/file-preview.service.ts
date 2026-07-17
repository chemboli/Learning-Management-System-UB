import { Injectable, signal } from '@angular/core';

export interface PreviewFile {
  fileName: string;
  url: string;
  contentType?: string | null;
}

/**
 * Drives a single, app-wide file preview modal (see FilePreviewComponent).
 * Any component can call open() with a download URL + filename/content-type
 * and the modal will pick the right viewer (PDF, image, video, audio, code
 * text) or fall back to a "download to view" message for unsupported types.
 */
@Injectable({ providedIn: 'root' })
export class FilePreviewService {
  current = signal<PreviewFile | null>(null);

  open(file: PreviewFile) {
    this.current.set(file);
  }

  close() {
    this.current.set(null);
  }
}
