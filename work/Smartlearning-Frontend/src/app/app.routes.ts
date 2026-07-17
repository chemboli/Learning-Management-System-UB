import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    // Public: invited users land here from the invitation email
    // (/activate?code=XXXX) to verify their code and set a password.
    path: 'activate',
    loadComponent: () =>
      import('./features/auth/activate/activate.component').then((m) => m.ActivateComponent)
  },

  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },

  {
    path: 'courses',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/courses/course-list/course-list.component').then((m) => m.CourseListComponent)
  },
  {
    path: 'courses/new',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN'] },
    loadComponent: () =>
      import('./features/courses/course-form/course-form.component').then((m) => m.CourseFormComponent)
  },
  {
    path: 'courses/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/courses/course-detail/course-detail.component').then(
        (m) => m.CourseDetailComponent
      )
  },
  {
    path: 'courses/:id/edit',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN'] },
    loadComponent: () =>
      import('./features/courses/course-form/course-form.component').then((m) => m.CourseFormComponent)
  },

  {
    path: 'my-courses',
    canActivate: [roleGuard],
    data: { roles: ['STUDENT'] },
    loadComponent: () =>
      import('./features/enrollments/my-courses/my-courses.component').then(
        (m) => m.MyCoursesComponent
      )
  },
  {
    path: 'enrollments',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN', 'LECTURER'] },
    loadComponent: () =>
      import('./features/enrollments/manage-enrollments/manage-enrollments.component').then(
        (m) => m.ManageEnrollmentsComponent
      )
  },

  {
    path: 'assignments/new',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN', 'LECTURER'] },
    loadComponent: () =>
      import('./features/assignments/assignment-form/assignment-form.component').then(
        (m) => m.AssignmentFormComponent
      )
  },
  {
    path: 'assignments/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/assignments/assignment-detail/assignment-detail.component').then(
        (m) => m.AssignmentDetailComponent
      )
  },
  {
    path: 'assignments/:id/edit',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN', 'LECTURER'] },
    loadComponent: () =>
      import('./features/assignments/assignment-form/assignment-form.component').then(
        (m) => m.AssignmentFormComponent
      )
  },

  {
    path: 'notes/upload',
    canActivate: [roleGuard],
    data: { roles: ['LECTURER', 'ADMIN', 'MASTER'] },
    loadComponent: () =>
      import('./features/notes/note-upload/note-upload.component').then((m) => m.NoteUploadComponent)
  },
  {
    path: 'notes/:id/edit',
    canActivate: [roleGuard],
    data: { roles: ['LECTURER', 'ADMIN', 'MASTER'] },
    loadComponent: () =>
      import('./features/notes/note-form/note-form.component').then((m) => m.NoteFormComponent)
  },

  {
    path: 'announcements',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/announcements/announcement-list/announcement-list.component').then(
        (m) => m.AnnouncementListComponent
      )
  },
  {
    path: 'announcements/new',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN', 'LECTURER'] },
    loadComponent: () =>
      import('./features/announcements/announcement-form/announcement-form.component').then(
        (m) => m.AnnouncementFormComponent
      )
  },
  {
    path: 'announcements/:id/edit',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN', 'LECTURER'] },
    loadComponent: () =>
      import('./features/announcements/announcement-form/announcement-form.component').then(
        (m) => m.AnnouncementFormComponent
      )
  },

  {
    path: 'users',
    canActivate: [roleGuard],
    data: { roles: ['MASTER', 'ADMIN'] },
    loadComponent: () =>
      import('./features/users/user-list/user-list.component').then((m) => m.UserListComponent)
  },
  {
    path: 'users/new',
    canActivate: [roleGuard],
    data: { roles: ['MASTER'] },
    loadComponent: () =>
      import('./features/users/user-form/user-form.component').then((m) => m.UserFormComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/users/profile/profile.component').then((m) => m.ProfileComponent)
  },

  { path: '**', redirectTo: 'dashboard' }
];
