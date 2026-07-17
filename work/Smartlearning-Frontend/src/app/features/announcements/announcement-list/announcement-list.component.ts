import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnnouncementService } from '../../../core/services/announcement.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { AnnouncementResponse } from '../../../core/models/models';

@Component({
  selector: 'app-announcement-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './announcement-list.component.html'
})
export class AnnouncementListComponent implements OnInit {
  announcements = signal<AnnouncementResponse[]>([]);
  loading = false;

  constructor(
    private announcementService: AnnouncementService,
    public auth: AuthService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.announcementService.getFeed().subscribe({
      next: (res) => {
        this.announcements.set(res);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  /**
   * Shows manage actions for anyone who might plausibly own this announcement;
   * the backend is the actual authority and will reject unauthorized edits/deletes.
   */
  canManage(a: AnnouncementResponse): boolean {
    return this.auth.hasAnyRole('ADMIN', 'MASTER', 'LECTURER');
  }

  priorityClass(priority: string): string {
    switch (priority) {
      case 'URGENT':
        return 'danger';
      case 'HIGH':
        return 'warning';
      case 'LOW':
        return 'secondary';
      default:
        return 'info';
    }
  }

  async delete(a: AnnouncementResponse) {
    const ok = await this.confirmService.ask(
      `Delete the announcement "${a.title}"? This cannot be undone.`,
      'Delete announcement'
    );

    if (!ok) return;

    this.announcementService.delete(a.id).subscribe({
      next: () => {
        this.toast.success('Announcement deleted.');
        this.announcements.update((list) => list.filter((x) => x.id !== a.id));
      }
    });
  }
}
