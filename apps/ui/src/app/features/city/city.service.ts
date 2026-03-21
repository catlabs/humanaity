import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap, startWith, map } from 'rxjs/operators';
import {
  AgentChatRequestInput,
  AgentChatResponseOutput,
  AgentChatService,
  CitiesService,
  EventOutput,
  HumansService,
  InventionOutput,
  SimulationsService,
  TimelineOutput,
  CityOutput,
  HumanOutput,
  CityOverviewOutput,
  SimulationCommandInput,
  SimulationCommandOutput,
  SimulationSnapshotOutput,
} from '@api';
import { parseApiResponse } from '@core';
import { SimulationAssistantResponse } from './simulation-assistant.models';

@Injectable({
  providedIn: 'root',
})
export class CityService {
  private http = inject(HttpClient);
  private agentChatService = inject(AgentChatService);
  private citiesService = inject(CitiesService);
  private humansService = inject(HumansService);
  private simulationsService = inject(SimulationsService);

  getCities(): Observable<CityOutput[]> {
    return this.citiesService
      .getAllCities()
      .pipe(switchMap(parseApiResponse<CityOutput[]>));
  }

  getCity(id: string): Observable<CityOutput> {
    return this.citiesService
      .getCityById(id)
      .pipe(switchMap(parseApiResponse<CityOutput>));
  }

  // Note: Subscriptions are not available in REST, using polling instead
  subscribePositions(cityId: string): Observable<HumanOutput[]> {
    // Poll every 100ms to simulate real-time updates
    return interval(100).pipe(
      startWith(0),
      switchMap(() =>
        this.humansService
          .getHumansByCity(cityId)
          .pipe(switchMap(parseApiResponse<HumanOutput[]>)),
      ),
    );
  }

  createCity(cityInput: { name: string }): Observable<CityOutput> {
    return this.citiesService
      .createCity(cityInput)
      .pipe(switchMap(parseApiResponse<CityOutput>));
  }

  getMyCities(): Observable<CityOutput[]> {
    return this.citiesService
      .getMyCities()
      .pipe(switchMap(parseApiResponse<CityOutput[]>));
  }

  startSimulation(cityId: number): Observable<void> {
    return this.simulationsService.startSimulation(cityId).pipe(
      switchMap(parseApiResponse<Record<string, string>>),
      map(() => void 0),
    );
  }

  stopSimulation(cityId: number): Observable<void> {
    return this.simulationsService.stopSimulation(cityId).pipe(
      switchMap(parseApiResponse<Record<string, string>>),
      map(() => void 0),
    );
  }

  isSimulationRunning(cityId: number): Observable<boolean> {
    return this.simulationsService.isSimulationRunning(cityId).pipe(
      switchMap(parseApiResponse<Record<string, boolean>>),
      map((response) => response['running'] ?? false),
    );
  }

  getSimulationOverview(): Observable<CityOverviewOutput[]> {
    return this.simulationsService
      .listCityOverviews()
      .pipe(switchMap(parseApiResponse<CityOverviewOutput[]>));
  }

  getSimulationSnapshot(cityId: number): Observable<SimulationSnapshotOutput> {
    return this.simulationsService
      .getCitySnapshot(cityId)
      .pipe(switchMap(parseApiResponse<SimulationSnapshotOutput>));
  }

  stepSimulation(cityId: number): Observable<void> {
    return this.simulationsService.stepSimulation(cityId).pipe(
      switchMap(parseApiResponse<Record<string, unknown>>),
      map(() => void 0),
    );
  }

  getSimulationTimeline(
    cityId: number,
    limit = 50,
  ): Observable<TimelineOutput> {
    return this.simulationsService
      .getCityTimeline(cityId, undefined, undefined, limit)
      .pipe(switchMap(parseApiResponse<TimelineOutput>));
  }

  getSimulationEvents(cityId: number, limit = 20): Observable<EventOutput[]> {
    return this.simulationsService
      .listCityEvents(cityId, undefined, undefined, limit)
      .pipe(switchMap(parseApiResponse<EventOutput[]>));
  }

  getSimulationInventions(
    cityId: number,
    limit = 20,
  ): Observable<InventionOutput[]> {
    return this.simulationsService
      .listCityInventions(cityId, undefined, undefined, limit)
      .pipe(switchMap(parseApiResponse<InventionOutput[]>));
  }

  sendAgentChat(
    cityId: number,
    request: AgentChatRequestInput,
  ): Observable<AgentChatResponseOutput> {
    return this.agentChatService
      .chat(cityId, request)
      .pipe(switchMap(parseApiResponse<AgentChatResponseOutput>));
  }

  sendSimulationCommand(
    cityId: number,
    request: SimulationCommandInput,
  ): Observable<SimulationCommandOutput> {
    return this.simulationsService
      .executeCommand(cityId, request)
      .pipe(switchMap(parseApiResponse<SimulationCommandOutput>));
  }

  sendSimulationAssistantCommand(
    cityId: number,
    commandText: string,
  ): Observable<SimulationAssistantResponse> {
    return this.http
      .post(`/api/simulations/${cityId}/assistant`, { commandText })
      .pipe(switchMap(parseApiResponse<SimulationAssistantResponse>));
  }

  deleteCity(id: number | string): Observable<void> {
    return this.citiesService.deleteCity(Number(id)).pipe(map(() => void 0));
  }
}
