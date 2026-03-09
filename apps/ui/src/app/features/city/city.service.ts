import {inject, Injectable, Inject, Optional} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, interval} from 'rxjs';
import {switchMap, startWith, map} from 'rxjs/operators';
import {CitiesService, HumansService, SimulationsService, CityOutput, HumanOutput, BASE_PATH} from '@api';
import {parseApiResponse} from '@core';

@Injectable({
  providedIn: 'root'
})
export class CityService {
  private citiesService = inject(CitiesService);
  private humansService = inject(HumansService);
  private simulationsService = inject(SimulationsService);
  private httpClient = inject(HttpClient);
  @Optional() @Inject(BASE_PATH) private basePath?: string;

  getCities(): Observable<CityOutput[]> {
    return this.citiesService.getAllCities().pipe(
      switchMap(parseApiResponse<CityOutput[]>)
    );
  }

  getCity(id: string): Observable<CityOutput> {
    return this.citiesService.getCityById(id).pipe(
      switchMap(parseApiResponse<CityOutput>)
    );
  }

  // Note: Subscriptions are not available in REST, using polling instead
  subscribePositions(cityId: string): Observable<HumanOutput[]> {
    // Poll every 100ms to simulate real-time updates
    return interval(100).pipe(
      startWith(0),
      switchMap(() => this.humansService.getHumansByCity(cityId).pipe(
        switchMap(parseApiResponse<HumanOutput[]>)
      ))
    );
  }

  createCity(cityInput: { name: string }): Observable<CityOutput> {
    return this.citiesService.createCity(cityInput).pipe(
      switchMap(parseApiResponse<CityOutput>)
    );
  }

  getMyCities(): Observable<CityOutput[]> {
    // Use HttpClient directly since the generated service might not have this endpoint yet
    // We'll call /api/cities/mine directly using the same basePath as other services
    const basePath = this.basePath || 'http://localhost:8080';
    const url = `${basePath}/api/cities/mine`;
    return this.httpClient.get<unknown>(url).pipe(
      switchMap(parseApiResponse<CityOutput[]>)
    );
  }

  startSimulation(cityId: number): Observable<void> {
    return this.simulationsService.startSimulation(cityId).pipe(
      switchMap(parseApiResponse<Record<string, string>>),
      map(() => void 0)
    );
  }

  stopSimulation(cityId: number): Observable<void> {
    return this.simulationsService.stopSimulation(cityId).pipe(
      switchMap(parseApiResponse<Record<string, string>>),
      map(() => void 0)
    );
  }

  isSimulationRunning(cityId: number): Observable<boolean> {
    return this.simulationsService.isSimulationRunning(cityId).pipe(
      switchMap(parseApiResponse<Record<string, boolean>>),
      map((response) => response['running'] ?? false)
    );
  }

  deleteCity(id: string): Observable<void> {
    return this.citiesService.deleteCity(id).pipe(
      map(() => void 0)
    );
  }
}
