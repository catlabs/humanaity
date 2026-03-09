import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '@features/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Skip adding token for auth endpoints
  if (req.url.includes('/auth/')) {
    return next(req);
  }

  // Add Bearer token to request
  const accessToken = authService.getAccessToken();
  if (accessToken) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // If 401 and we have a refresh token, try to refresh
      if (error.status === 401 && authService.getRefreshToken()) {
        return authService.refreshToken().pipe(
          switchMap((response) => {
            // Retry the original request with new token
            const clonedReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${response.accessToken}`
              }
            });
            return next(clonedReq);
          }),
          catchError((refreshError) => {
            // Refresh failed, logout and redirect
            authService.logout();
            router.navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }

      // If 401 without refresh token, redirect to login
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};

