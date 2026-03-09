import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, tap, map, switchMap } from 'rxjs';
import { AuthControllerService, AuthRequest, SignupRequest, RefreshTokenRequest, AuthResponse } from '@api';
import { parseApiResponse } from '@core';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authControllerService = inject(AuthControllerService);
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  login(request: AuthRequest): Observable<AuthResponse> {
    return this.authControllerService.login(request).pipe(
      switchMap(parseApiResponse<Record<string, unknown>>),
      map((response) => {
        // Extract tokens
        const accessToken =
          (typeof response['accessToken'] === 'string' ? response['accessToken'] : undefined) ??
          (typeof response['access_token'] === 'string' ? response['access_token'] : undefined);
        const refreshToken =
          (typeof response['refreshToken'] === 'string' ? response['refreshToken'] : undefined) ??
          (typeof response['refresh_token'] === 'string' ? response['refresh_token'] : undefined);

        if (!accessToken || !refreshToken) {
          throw new Error('Invalid response structure: tokens not found');
        }

        return { accessToken, refreshToken };
      }),
      tap(response => {
        this.setTokens(response);
      })
    );
  }

  signup(request: SignupRequest): Observable<AuthResponse> {
    return this.authControllerService.signup(request).pipe(
      switchMap(parseApiResponse<Record<string, unknown>>),
      map((response) => {
        const accessToken =
          (typeof response['accessToken'] === 'string' ? response['accessToken'] : undefined) ??
          (typeof response['access_token'] === 'string' ? response['access_token'] : undefined);
        const refreshToken =
          (typeof response['refreshToken'] === 'string' ? response['refreshToken'] : undefined) ??
          (typeof response['refresh_token'] === 'string' ? response['refresh_token'] : undefined);

        if (!accessToken || !refreshToken) {
          throw new Error('Invalid response structure: tokens not found');
        }

        return { accessToken, refreshToken };
      }),
      tap(response => this.setTokens(response))
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const request: RefreshTokenRequest = { refreshToken };
    return this.authControllerService.refresh(request).pipe(
      switchMap(parseApiResponse<Record<string, unknown>>),
      map((response) => {
        const accessToken =
          (typeof response['accessToken'] === 'string' ? response['accessToken'] : undefined) ??
          (typeof response['access_token'] === 'string' ? response['access_token'] : undefined);
        const refreshToken =
          (typeof response['refreshToken'] === 'string' ? response['refreshToken'] : undefined) ??
          (typeof response['refresh_token'] === 'string' ? response['refresh_token'] : undefined);

        if (!accessToken || !refreshToken) {
          throw new Error('Invalid response structure: tokens not found');
        }

        return { accessToken, refreshToken };
      }),
      tap(response => this.setTokens(response))
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      const request: RefreshTokenRequest = { refreshToken };
      this.authControllerService.logout(request).subscribe();
    }
    this.clearTokens();
  }

  getAccessToken(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    if (!this.isBrowser) {
      return false;
    }
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }

    // Simple check: decode and verify expiration
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiration = payload.exp * 1000; // Convert to milliseconds
      return Date.now() < expiration;
    } catch {
      return false;
    }
  }

  private setTokens(response: AuthResponse): void {
    if (!this.isBrowser) {
      return;
    }
    if (!response.accessToken || !response.refreshToken) {
      throw new Error('Invalid authentication response: missing tokens');
    }
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
  }

  private clearTokens(): void {
    if (!this.isBrowser) {
      return;
    }
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}
