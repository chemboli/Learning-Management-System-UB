import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      let message = 'Something went wrong. Please try again.';

      if (err.error) {
        if (typeof err.error === 'string') {
          message = err.error;
        } else if (err.error.message) {
          message = err.error.message;
        } else if (err.error.error) {
          message = err.error.error;
        }
      }

      if (err.status === 0) {
        message = 'Cannot reach the server. Check your connection.';
      } else if (err.status === 401) {
        message = 'Your session has expired. Please sign in again.';
        auth.logout();
        router.navigate(['/login']);
      } else if (err.status === 403) {
        message = "You don't have permission to do that.";
      }

      toast.error(message);
      return throwError(() => err);
    })
  );
};
