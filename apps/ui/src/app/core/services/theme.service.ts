import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

type ThemePreference = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);

  private readonly storageKey = 'humanaity.theme';

  /** Effective theme state applied to the DOM. */
  readonly isDark = signal(false);

  private mediaQueryList: MediaQueryList | null = null;
  private systemListener: ((e: MediaQueryListEvent) => void) | null = null;

  init(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const stored = this.getStoredPreference();
    if (stored) {
      this.applyPreference(stored);
      return;
    }

    // Default: follow system preference.
    this.mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)');
    this.applyIsDark(this.mediaQueryList.matches);

    this.systemListener = (e: MediaQueryListEvent) => {
      // If the user set an override since init, don't fight it.
      if (this.getStoredPreference()) return;
      this.applyIsDark(e.matches);
    };

    // Modern browsers
    this.mediaQueryList.addEventListener?.('change', this.systemListener);
    // Safari < 14
    // eslint-disable-next-line deprecation/deprecation
    this.mediaQueryList.addListener?.(this.systemListener);
  }

  toggle(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const next: ThemePreference = this.isDark() ? 'light' : 'dark';
    this.setStoredPreference(next);
    this.applyPreference(next);
  }

  clearOverride(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.clearStoredPreference();
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.applyIsDark(prefersDark);
  }

  private applyPreference(pref: ThemePreference): void {
    this.applyIsDark(pref === 'dark');
  }

  private applyIsDark(isDark: boolean): void {
    this.isDark.set(isDark);
    const root = this.document.documentElement;
    root.classList.toggle('dark', isDark);
  }

  private getStoredPreference(): ThemePreference | null {
    try {
      const value = window.localStorage.getItem(this.storageKey);
      if (value === 'light' || value === 'dark') return value;
      return null;
    } catch {
      return null;
    }
  }

  private setStoredPreference(value: ThemePreference): void {
    try {
      window.localStorage.setItem(this.storageKey, value);
    } catch {
      // ignore (private mode / storage disabled)
    }
  }

  private clearStoredPreference(): void {
    try {
      window.localStorage.removeItem(this.storageKey);
    } catch {
      // ignore
    }
  }
}

