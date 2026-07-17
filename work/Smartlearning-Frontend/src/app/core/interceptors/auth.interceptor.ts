import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  // environment.apiUrl can be relative ("/api", in dev via the proxy) or a
  // full absolute URL (in prod, since the frontend and backend are on
  // different Render domains). Either way, only requests actually going to
  // OUR backend should get the JWT attached. Everything else — most notably
  // MinIO/R2's presigned file URLs used for downloads and previews —
  // authenticates via its own signed query parameters and must NOT get our
  // JWT attached, or the request gets rejected by that other server.
  const isOwnApi = req.url.startsWith(environment.apiUrl);

  if (token && isOwnApi && !req.url.includes('/auth/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req);
};

