import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/models';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const allowed = (route.data['roles'] as Role[]) ?? [];

  if (allowed.length === 0 || auth.hasAnyRole(...allowed)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
