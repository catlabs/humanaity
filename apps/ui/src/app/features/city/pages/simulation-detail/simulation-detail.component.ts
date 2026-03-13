import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import {
  CityOutput,
  EventOutput,
  HumanOutput,
  InventionOutput,
  SimulationSnapshotOutput,
} from '@api';
import { EventItemComponent, EventType } from '@shared';
import { Observable, Subscription, interval } from 'rxjs';
import { CityService } from '../../city.service';
import { PixiCanvasService } from '../../services/pixi-canvas.service';

@Component({
  selector: 'app-simulation-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatDividerModule,
    MatSidenavModule,
    MatListModule,
    MatProgressSpinnerModule,
    EventItemComponent,
  ],
  templateUrl: './simulation-detail.component.html',
  styleUrl: './simulation-detail.component.scss',
})
export class SimulationDetailComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  @ViewChild('worldCanvas', { static: false })
  worldCanvasRef?: ElementRef<HTMLDivElement>;

  private route = inject(ActivatedRoute);
  private cityService = inject(CityService);
  private pixiCanvasService = inject(PixiCanvasService);

  private pollingSubscription?: Subscription;
  private pixiInitialized = false;

  city: CityOutput = this.route.snapshot.data['city'];

  readonly humanStates = [
    'active',
    'creating',
    'collaborating',
    'contemplating',
    'resting',
  ] as const;

  filters = signal<Record<string, boolean>>({
    active: true,
    resting: true,
    creating: true,
    collaborating: true,
    contemplating: true,
  });

  humans = signal<SimulationSnapshotOutput['humans']>([]);
  inventions = signal<InventionOutput[]>([]);
  events = signal<EventOutput[]>([]);

  selectedHumanId = signal<number | null>(null);
  selectedInventionId = signal<number | null>(null);
  selectedEventId = signal<number | null>(null);

  snapshotLoading = signal(true);
  historyLoading = signal(true);
  controlBusy = signal(false);
  snapshotError = signal<string | null>(null);
  historyError = signal<string | null>(null);

  hasRun = signal(false);
  isRunning = signal(false);
  currentTick = signal(0);
  currentYear = signal(1);
  currentEra = signal('FOUNDING');
  worldPhase = signal('CREATED');

  populationTotal = signal(0);
  populationBusy = signal(0);
  totalEvents = signal(0);
  totalInventions = signal(0);
  recentEventCount = signal(0);
  recentInventionCount = signal(0);

  filteredHumans = computed(() =>
    this.humans().filter((human) => this.filters()[this.humanState(human)])
  );

  selectedHuman = computed(() => {
    const id = this.selectedHumanId();
    return id === null ? null : this.humans().find((human) => human.id === id) ?? null;
  });

  selectedInvention = computed(() => {
    const id = this.selectedInventionId();
    return id === null
      ? null
      : this.inventions().find((invention) => invention.id === id) ?? null;
  });

  selectedEvent = computed(() => {
    const id = this.selectedEventId();
    return id === null ? null : this.events().find((event) => event.id === id) ?? null;
  });

  populationActive = computed(() =>
    Math.max(this.populationTotal() - this.populationBusy(), 0)
  );

  inventionCounts = computed(() => {
    const inventions = this.inventions();
    return {
      scientific: inventions.filter((inv) => inv.category === 'TECHNIQUE').length,
      philosophical: inventions.filter((inv) => inv.category === 'KNOWLEDGE').length,
      cultural: inventions.filter((inv) => inv.category === 'SOCIAL_PRACTICE').length,
      total: inventions.length,
    };
  });

  eraLabel = computed(() => this.formatEnumLabel(this.currentEra()));
  phaseLabel = computed(() => this.formatEnumLabel(this.worldPhase()));
  canStart = computed(() => !this.controlBusy() && !this.isRunning());
  canPause = computed(() => !this.controlBusy() && this.isRunning());
  canStep = computed(
    () => !this.controlBusy() && this.hasRun() && !this.isRunning()
  );
  noRunYet = computed(() => !this.snapshotLoading() && !this.hasRun());
  hasHumans = computed(() => this.populationTotal() > 0);
  showWorldOverlay = computed(() => this.noRunYet() || !this.hasHumans());

  ngOnInit(): void {
    this.refreshAll();
    this.pollingSubscription = interval(2000).subscribe(() => {
      if (this.isRunning()) {
        this.refreshAll(false);
      }
    });
  }

  async ngAfterViewInit(): Promise<void> {
    const container = this.worldCanvasRef?.nativeElement;
    if (!container) {
      return;
    }

    await this.pixiCanvasService.initialize(container);
    this.pixiInitialized = true;
    this.syncPixiHumans();
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.pixiCanvasService.destroy();
  }

  toggleFilter(state: string): void {
    this.filters.update((prev) => ({ ...prev, [state]: !prev[state] }));
  }

  onStart(): void {
    this.runControlAction((cityId) => this.cityService.startSimulation(cityId));
  }

  onPause(): void {
    this.runControlAction((cityId) => this.cityService.stopSimulation(cityId));
  }

  onStep(): void {
    this.runControlAction((cityId) => this.cityService.stepSimulation(cityId));
  }

  onRefresh(): void {
    if (this.controlBusy()) {
      return;
    }
    this.refreshAll();
  }

  selectHuman(human: SimulationSnapshotOutput['humans'][number]): void {
    this.selectedHumanId.set(human.id);
    this.selectedInventionId.set(null);
    this.selectedEventId.set(null);
  }

  selectInvention(invention: InventionOutput): void {
    this.selectedInventionId.set(invention.id);
    this.selectedHumanId.set(null);
    this.selectedEventId.set(null);
  }

  selectEvent(event: EventOutput): void {
    this.selectedEventId.set(event.id);
    this.selectedHumanId.set(null);
    this.selectedInventionId.set(null);
  }

  clearSelection(): void {
    this.selectedHumanId.set(null);
    this.selectedInventionId.set(null);
    this.selectedEventId.set(null);
  }

  humanState(human: SimulationSnapshotOutput['humans'][number]): string {
    return human.busy ? 'resting' : 'active';
  }

  humanPrimaryTrait(human: SimulationSnapshotOutput['humans'][number]): string {
    return human.busy ? 'Focused' : 'Curious';
  }

  eventType(event: EventOutput): EventType {
    const typeMap: Record<EventOutput.EventTypeEnum, EventType> = {
      SIMULATION_STARTED: 'other',
      SIMULATION_PAUSED: 'other',
      SIMULATION_RESUMED: 'other',
      SIMULATION_COMPLETED: 'other',
      HUMANS_COLLIDED: 'warning',
      DISCOVERY_UNLOCKED: 'invention',
      DIALOGUE_EXCHANGED: 'other',
      INVENTION_EMERGED: 'invention',
    };

    return typeMap[event.eventType] ?? 'other';
  }

  eventTitle(event: EventOutput): string {
    return this.formatEnumLabel(event.eventType);
  }

  eventDescription(event: EventOutput): string {
    return `Tick ${event.tick} • Year ${event.year} • ${this.formatEnumLabel(event.era)}`;
  }

  eventNarrative(event: EventOutput): string | null {
    return event.enrichedSnippet?.trim() || null;
  }

  eventEnrichmentStatusLabel(event: EventOutput): string {
    return this.formatEnumLabel(event.enrichmentStatus);
  }

  eventTimestamp(event: EventOutput): string {
    return new Date(event.createdAt).toLocaleString();
  }

  inventionCategoryLabel(invention: InventionOutput): string {
    return this.formatEnumLabel(invention.category);
  }

  inventionDisplayTitle(invention: InventionOutput): string {
    return invention.enrichedTitle?.trim() || invention.title;
  }

  inventionDisplaySummary(invention: InventionOutput): string {
    return invention.enrichedSummary?.trim() || invention.summary;
  }

  inventionEnrichmentStatusLabel(invention: InventionOutput): string {
    return this.formatEnumLabel(invention.enrichmentStatus);
  }

  private refreshAll(showLoading = true): void {
    this.refreshSnapshot(showLoading);
    this.refreshHistory(showLoading);
  }

  private refreshSnapshot(showLoading: boolean): void {
    const cityId = this.requireCityId();
    if (!cityId) {
      return;
    }

    if (showLoading) {
      this.snapshotLoading.set(true);
    }
    this.snapshotError.set(null);

    this.cityService.getSimulationSnapshot(cityId).subscribe({
      next: (snapshot) => {
        this.applySnapshot(snapshot);
        this.snapshotLoading.set(false);
      },
      error: (error) => {
        this.snapshotLoading.set(false);
        this.snapshotError.set('Failed to load simulation snapshot.');
        console.error('Error loading simulation snapshot:', error);
      },
    });
  }

  private refreshHistory(showLoading: boolean): void {
    const cityId = this.requireCityId();
    if (!cityId) {
      return;
    }

    if (showLoading) {
      this.historyLoading.set(true);
    }
    this.historyError.set(null);

    this.cityService.getSimulationTimeline(cityId, 100).subscribe({
      next: (timeline) => {
        this.events.set(timeline.events);
        this.inventions.set(timeline.inventions);
        this.totalEvents.set(timeline.eventCount);
        this.totalInventions.set(timeline.inventionCount);
        this.historyLoading.set(false);
      },
      error: (error) => {
        this.historyLoading.set(false);
        this.historyError.set('Failed to load simulation history.');
        console.error('Error loading simulation timeline:', error);
      },
    });
  }

  private applySnapshot(snapshot: SimulationSnapshotOutput): void {
    this.hasRun.set(snapshot.run.hasRun);
    this.isRunning.set(snapshot.run.running);
    this.currentTick.set(snapshot.run.tick);
    this.currentYear.set(snapshot.run.year);
    this.currentEra.set(snapshot.run.era);
    this.worldPhase.set(
      snapshot.run.status ?? (snapshot.run.running ? 'RUNNING' : 'CREATED')
    );

    this.populationTotal.set(snapshot.metrics.population);
    this.populationBusy.set(snapshot.metrics.busyCount);
    this.totalEvents.set(snapshot.metrics.eventCount);
    this.totalInventions.set(snapshot.metrics.inventionCount);
    this.recentEventCount.set(snapshot.timelineSummary.recentEventCount);
    this.recentInventionCount.set(snapshot.timelineSummary.recentInventionCount);
    this.humans.set(snapshot.humans);

    if (this.events().length === 0 && snapshot.recentEvents.length > 0) {
      this.events.set(snapshot.recentEvents);
    }
    if (this.inventions().length === 0 && snapshot.recentInventions.length > 0) {
      this.inventions.set(snapshot.recentInventions);
    }

    this.syncPixiHumans(
      snapshot.humans.map((human) => ({
        id: human.id,
        name: human.name,
        x: human.x ?? 0,
        y: human.y ?? 0,
        busy: human.busy,
      }))
    );
  }

  private syncPixiHumans(snapshotHumans = this.humansToOutput()): void {
    if (!this.pixiInitialized) {
      return;
    }

    snapshotHumans.forEach((human) => {
      this.pixiCanvasService.addHuman(human);
      this.pixiCanvasService.updateHuman(human);
    });
  }

  private humansToOutput(): HumanOutput[] {
    return this.humans().map((human) => ({
      id: human.id,
      name: human.name,
      x: human.x ?? 0,
      y: human.y ?? 0,
      busy: human.busy,
    }));
  }

  private runControlAction(action: (cityId: number) => Observable<unknown>): void {
    const cityId = this.requireCityId();
    if (!cityId || this.controlBusy()) {
      return;
    }

    this.controlBusy.set(true);
    action(cityId).subscribe({
      next: () => {
        this.controlBusy.set(false);
        this.refreshAll(false);
      },
      error: (error: unknown) => {
        this.controlBusy.set(false);
        this.snapshotError.set('Simulation control action failed.');
        console.error('Simulation control action failed:', error);
      },
    });
  }

  private requireCityId(): number | null {
    if (!this.city.id) {
      this.snapshotError.set('Missing city id.');
      return null;
    }
    return this.city.id;
  }

  private formatEnumLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
