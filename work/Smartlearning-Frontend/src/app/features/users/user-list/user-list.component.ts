import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { UserResponse } from '../../../core/models/models';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {
  users = signal<UserResponse[]>([]);
  loading = false;
  searchTerm = '';
  roleFilter = '';

  constructor(
    private userService: UserService,
    public auth: AuthService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (res) => {
        this.users.set(res);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  filteredUsers() {
    const term = this.searchTerm.trim().toLowerCase();

    return this.users().filter((u) => {
      const matchesTerm =
        !term ||
        u.email.toLowerCase().includes(term) ||
        `${u.firstName} ${u.lastName}`.toLowerCase().includes(term) ||
        (u.matricule ?? '').toLowerCase().includes(term);

      const matchesRole = !this.roleFilter || u.role === this.roleFilter;

      return matchesTerm && matchesRole;
    });
  }

  exportingCsv = false;

  /**
   * Exports exactly what's currently visible in the table. The role filter and
   * search box are both client-side, so when either is active we build the CSV
   * from the already-filtered rows directly rather than re-querying the server
   * (which only knows how to filter by role, not by search text).
   */
  exportCsv() {
    const rows = this.filteredUsers();

    if (rows.length === 0) {
      this.toast.error('There are no users matching the current filters to export.');
      return;
    }

    const hasActiveFilter = !!this.searchTerm.trim() || !!this.roleFilter;

    if (!hasActiveFilter) {
      // No filters active — just download everyone directly from the server.
      this.exportingCsv = true;
      this.userService.exportUsersCsv(null).subscribe({
        next: (blob) => {
          this.exportingCsv = false;
          this.triggerDownload(blob, 'users-all.csv');
        },
        error: () => {
          this.exportingCsv = false;
          this.toast.error('Could not generate the CSV export.');
        }
      });
      return;
    }

    const csv = this.buildCsv(rows);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const suffix = this.roleFilter ? `-${this.roleFilter.toLowerCase()}` : '-filtered';
    this.triggerDownload(blob, `users${suffix}.csv`);
  }

  private buildCsv(rows: UserResponse[]): string {
    const header = ['First Name', 'Last Name', 'Email', 'Matricule', 'Role', 'Enabled'];
    const escape = (value: string) => {
      if (/[",\n\r]/.test(value)) {
        return `"${value.replace(/"/g, '""')}"`;
      }
      return value;
    };

    const lines = [header.map(escape).join(',')];
    for (const u of rows) {
      lines.push(
        [u.firstName, u.lastName, u.email, u.matricule ?? '', u.role, u.enabled ? 'Yes' : 'No']
          .map((v) => escape(String(v)))
          .join(',')
      );
    }

    // BOM for Excel, matching the backend export's encoding.
    return '\uFEFF' + lines.join('\r\n') + '\r\n';
  }

  private triggerDownload(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  /** ID of the user whose invitation is currently being re-sent (for the per-row spinner). */
  resendingId: string | null = null;

  resendInvitation(u: UserResponse) {
    this.resendingId = u.id;

    this.userService.resendInvitation(u.id).subscribe({
      next: () => {
        this.resendingId = null;
        this.toast.success(`Invitation re-sent to ${u.email}.`);
      },
      error: () => (this.resendingId = null)
    });
  }

  async deleteUser(u: UserResponse) {
    const ok = await this.confirmService.ask(
      `Delete ${u.firstName} ${u.lastName}'s account? This cannot be undone.`,
      'Delete user',
      'Delete'
    );

    if (!ok) return;

    this.userService.deleteUser(u.id).subscribe({
      next: () => {
        this.users.update((list) => list.filter((x) => x.id !== u.id));
        this.toast.success('User deleted.');
      }
    });
  }
}
