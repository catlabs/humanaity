import { from, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * Parse OpenAPI-generated service responses when using fetch.
 *
 * When `provideHttpClient(withFetch())` is enabled, OpenAPI generator may return `Blob`
 * instead of already-parsed JSON. This helper centralizes the conversion to avoid
 * duplicating Blob/FileReader logic across services and components.
 */
export function parseApiResponse<T>(response: unknown): Observable<T> {
  const isBlob = typeof Blob !== 'undefined' && response instanceof Blob;

  if (isBlob) {
    return from((response as Blob).text()).pipe(
      map((text) => JSON.parse(text) as T)
    );
  }

  if (typeof response === 'string') {
    return of(JSON.parse(response) as T);
  }

  return of(response as T);
}

