import { Component, computed, effect, inject, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer } from '@angular/platform-browser';
import { FilePreviewService } from '../../../core/services/file-preview.service';

type PreviewKind = 'pdf' | 'image' | 'video' | 'audio' | 'text' | 'office' | 'unsupported';

@Component({
  selector: 'app-file-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-preview.component.html',
  styleUrl: './file-preview.component.scss'
})
export class FilePreviewComponent implements OnDestroy {
  private sanitizer = inject(DomSanitizer);
  private http = inject(HttpClient);
  previewService = inject(FilePreviewService);

  file = this.previewService.current;

  textContent = signal<string | null>(null);
  textLoading = signal(false);
  textError = signal(false);

  // PDFs are fetched as a blob and rendered from a same-origin blob: URL —
  // loading a presigned MinIO URL directly in an <iframe> is unreliable across
  // browsers (cross-origin PDF rendering inside iframes is inconsistent, and
  // MinIO doesn't set Content-Disposition: inline by default).
  pdfBlobUrl = signal<string | null>(null);
  pdfLoading = signal(false);
  pdfError = signal(false);
  private currentBlobUrl: string | null = null;

  kind = computed<PreviewKind>(() => {
    const f = this.file();
    if (!f) return 'unsupported';
    return this.detectKind(f.contentType, f.fileName);
  });

  safePdfUrl = computed(() => {
    const url = this.pdfBlobUrl();
    if (!url) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  constructor() {
    effect(() => {
      const f = this.file();
      const k = this.kind();

      this.textContent.set(null);
      this.textError.set(false);
      this.pdfError.set(false);
      this.releaseBlobUrl();

      if (!f) return;

      if (k === 'text') {
        this.textLoading.set(true);
        this.http.get(f.url, { responseType: 'text' }).subscribe({
          next: (text) => {
            this.textContent.set(text);
            this.textLoading.set(false);
          },
          error: () => {
            this.textError.set(true);
            this.textLoading.set(false);
          }
        });
      }

      if (k === 'pdf') {
        this.pdfLoading.set(true);
        this.http.get(f.url, { responseType: 'blob' }).subscribe({
          next: (blob) => {
            const typedBlob = blob.type ? blob : blob.slice(0, blob.size, 'application/pdf');
            this.currentBlobUrl = URL.createObjectURL(typedBlob);
            this.pdfBlobUrl.set(this.currentBlobUrl);
            this.pdfLoading.set(false);
          },
          error: () => {
            this.pdfError.set(true);
            this.pdfLoading.set(false);
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.releaseBlobUrl();
  }

  close() {
    this.previewService.close();
  }

  private releaseBlobUrl() {
    if (this.currentBlobUrl) {
      URL.revokeObjectURL(this.currentBlobUrl);
      this.currentBlobUrl = null;
    }
    this.pdfBlobUrl.set(null);
  }

  private detectKind(contentType: string | null | undefined, fileName: string): PreviewKind {
    const ct = (contentType || '').toLowerCase();
    const ext = (fileName.split('.').pop() || '').toLowerCase();

    if (ct === 'application/pdf' || ext === 'pdf') return 'pdf';

    if (ct.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp'].includes(ext)) {
      return 'image';
    }

    if (ct.startsWith('video/') || ['mp4', 'webm', 'ogg', 'mov', 'mkv'].includes(ext)) {
      return 'video';
    }

    if (ct.startsWith('audio/') || ['mp3', 'wav', 'm4a', 'aac'].includes(ext)) {
      return 'audio';
    }

    if (
      ct.startsWith('text/') ||
      ct === 'application/json' ||
      ['txt', 'java', 'py', 'c', 'cpp', 'h', 'hpp', 'js', 'ts', 'json', 'md', 'csv'].includes(ext)
    ) {
      return 'text';
    }

    // Word / Excel / PowerPoint — browsers can't render these natively, so we
    // show a clear explanation instead of silently failing like a generic
    // "unsupported" file would.
    if (
      ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].includes(ext) ||
      ct.includes('msword') ||
      ct.includes('officedocument')
    ) {
      return 'office';
    }

    return 'unsupported';
  }
}
