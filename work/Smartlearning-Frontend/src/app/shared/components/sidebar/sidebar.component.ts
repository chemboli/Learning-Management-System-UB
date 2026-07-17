import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  link?: string;
  roles?: string[];
  children?: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent {
  /** Whether the sidebar is collapsed to icon-only (desktop) or hidden (mobile). */
  collapsed = signal(false);
  mobileOpen = signal(false);

  openSections = signal<Set<string>>(new Set(['Academics']));

  navSections: NavItem[] = [
    {
      label: 'Academics',
      icon: 'bi-mortarboard',
      children: [
        { label: 'Dashboard', icon: 'bi-speedometer2', link: '/dashboard' },
        { label: 'Courses', icon: 'bi-journal-bookmark', link: '/courses' },
        { label: 'My courses', icon: 'bi-collection', link: '/my-courses', roles: ['STUDENT'] }
      ]
    },
    {
      label: 'Announcements',
      icon: 'bi-megaphone',
      children: [{ label: 'All announcements', icon: 'bi-megaphone-fill', link: '/announcements' }]
    },
    {
      label: 'Content',
      icon: 'bi-cloud-upload',
      roles: ['LECTURER', 'ADMIN', 'MASTER'],
      children: [
        { label: 'Upload note', icon: 'bi-cloud-upload', link: '/notes/upload', roles: ['LECTURER', 'ADMIN', 'MASTER'] },
        { label: 'New assignment', icon: 'bi-clipboard-plus', link: '/assignments/new', roles: ['LECTURER', 'ADMIN', 'MASTER'] },
        { label: 'Enrollments', icon: 'bi-clipboard-check', link: '/enrollments', roles: ['LECTURER', 'ADMIN', 'MASTER'] }
      ]
    },
    {
      label: 'Administration',
      icon: 'bi-gear',
      roles: ['ADMIN', 'MASTER'],
      children: [
        { label: 'Users', icon: 'bi-people', link: '/users', roles: ['ADMIN', 'MASTER'] }
      ]
    }
  ];

  constructor(public auth: AuthService, private router: Router) {}

  visibleSections(): NavItem[] {
    return this.navSections
      .filter((s) => !s.roles || this.auth.hasAnyRole(...(s.roles as any)))
      .map((s) => ({
        ...s,
        children: (s.children ?? []).filter((c) => !c.roles || this.auth.hasAnyRole(...(c.roles as any)))
      }))
      .filter((s) => (s.children?.length ?? 0) > 0);
  }

  isSectionOpen(label: string): boolean {
    return this.openSections().has(label);
  }

  toggleSection(label: string) {
    this.openSections.update((set) => {
      const next = new Set(set);
      if (next.has(label)) {
        next.delete(label);
      } else {
        next.add(label);
      }
      return next;
    });
  }

  toggleCollapsed() {
    this.collapsed.update((v) => !v);
  }

  toggleMobile() {
    this.mobileOpen.update((v) => !v);
  }

  closeMobile() {
    this.mobileOpen.set(false);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
