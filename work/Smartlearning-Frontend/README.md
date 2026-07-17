# SmartLearning — Frontend

Angular 18 (standalone components) + Bootstrap 5 client for the SmartLearning Spring Boot API. Sky-blue themed.

## Stack
- Angular 18, standalone components, signals for local state
- Bootstrap 5 + Bootstrap Icons (themed via SCSS variables in `src/styles.scss`)
- Reactive forms
- JWT bearer auth via an HTTP interceptor

## Getting started

```bash
npm install
npm start
```

This serves the app on `http://localhost:4200` and proxies all `/api/**` calls to `http://localhost:8080` (see `proxy.conf.json`), so just run the Spring Boot backend on port 8080 alongside it.

To point at a different backend host, edit `proxy.conf.json` or `src/environments/environment.ts`.

## Production build

```bash
npm run build
```

Output goes to `dist/smartlearning-frontend`. If you serve the frontend from a different origin than the API in production, **enable CORS on the Spring Boot backend** (there's currently no CORS configuration) or serve both behind the same reverse proxy / origin.

## Structure

```
src/app/
  core/
    models/        Shared TS interfaces mirroring backend DTOs
    services/       AuthService, UserService, CourseService, EnrollmentService, NoteService, ToastService
    interceptors/    JWT attach + global error → toast handling
    guards/          authGuard (must be logged in), roleGuard (role allow-list via route data)
  shared/components/
    navbar/          Top nav, role-aware links
    toast/           Toast notifications
    confirm-dialog/   Promise-based confirm() modal for destructive actions
  features/
    auth/            login, register
    dashboard/        role-aware home page
    courses/          list, detail (+ notes + enroll/unenroll), create/edit form
    enrollments/      my-courses (student), manage-enrollments (admin, filter by course)
    notes/            upload, edit metadata
    users/            list (admin), create (master only), profile (self-service + password change)
```

## Roles
`USER`, `STUDENT`, `LECTURER`, `ADMIN`, `MASTER` — matches the backend `Role` enum. Route access and UI visibility are gated via `roleGuard` and `AuthService.hasAnyRole(...)`, mirroring the `@PreAuthorize` rules on the backend controllers. Note that user deletion and admin-created accounts are restricted to `MASTER` only, matching the backend.

## Auth notes
The backend JWT only encodes the subject (email) — role isn't a claim — so the frontend stores `email`/`role` from the `/auth/login` and `/auth/register` response body (and refreshes them from `/users/me` once loaded) rather than decoding the token for role info.
