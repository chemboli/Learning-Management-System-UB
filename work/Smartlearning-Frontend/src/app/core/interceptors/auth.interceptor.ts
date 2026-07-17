import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  // Our own API is always called with a relative URL (see environment.apiUrl).
  // Any absolute URL (http:// or https://) points to a different host entirely —
  // most notably MinIO's presigned file URLs, used for downloads and previews.
  // Those authenticate via their own signed query parameters and must NOT get
  // our JWT attached, or the request gets rejected by that other server.
  const isAbsoluteUrl = /^https?:\/\//i.test(req.url);

  if (token && !isAbsoluteUrl && !req.url.includes('/auth/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req);
};
