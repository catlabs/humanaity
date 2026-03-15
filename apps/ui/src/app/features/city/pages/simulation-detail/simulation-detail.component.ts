import { CommonModule } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import {
  AgentChatRequestInput,
  AgentChatResponseOutput,
  AgentUiEffectOutput,
  CityOutput,
  EventOutput,
  InventionOutput,
  SimulationSnapshotOutput,
} from '@api';
import { EventType } from '@shared';
import { Observable, Subscription, interval } from 'rxjs';
import { CityService } from '../../city.service';
import { SimulationBoardComponent } from '../../components/simulation-board/simulation-board.component';
import { AgentChatEffectsService } from '../../services/agent-chat-effects.service';
import {
  BoardPlaceViewModel,
  BoardViewModelService,
} from '../../services/board-view-model.service';

@Component({
  selector: 'app-simulation-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    SimulationBoardComponent,
  ],
  templateUrl: './simulation-detail.component.html',
  styleUrl: './simulation-detail.component.scss',
})
export class SimulationDetailComponent
  implements OnInit, OnDestroy
{
  private route = inject(ActivatedRoute);
  private cityService = inject(CityService);
  private agentChatEffectsService = inject(AgentChatEffectsService);
  private boardViewModelService = inject(BoardViewModelService);

  private pollingSubscription?: Subscription;

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
  trackedHumanId = signal<number | null>(null);
  guidedFocus = signal<GuidedHumanSummary | null>(null);
  guidedComparison = signal<GuidedCompareSummary | null>(null);
  guidedFollow = signal<GuidedFollowSummary | null>(null);
  pendingDirectorConfirmation = signal<DirectorConfirmationSummary | null>(null);
  boardEventEntries = signal<BoardEventEntry[]>([]);
  boardPulseNonce = signal(0);
  lastBoardReaction = signal<string | null>(null);
  directorBoardState = signal<'pending' | 'executed' | null>(null);
  chatInput = signal('');
  chatBusy = signal(false);
  chatError = signal<string | null>(null);
  chatConversationId = signal<string | null>(null);
  chatEntries = signal<ChatEntry[]>([]);

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
  boardMarkers = computed(() =>
    this.boardViewModelService.fromHumans(this.humans()).markers
  );
  boardPlaces = computed<BoardPlaceViewModel[]>(() => [
    { id: 'forest', label: 'Forest', icon: '🌳', leftPct: 14, topPct: 18 },
    { id: 'river', label: 'River', icon: '🌊', leftPct: 82, topPct: 22 },
    { id: 'church', label: 'Church', icon: '⛪', leftPct: 52, topPct: 30 },
    { id: 'campfire', label: 'Campfire', icon: '🔥', leftPct: 34, topPct: 72 },
    { id: 'house', label: 'House', icon: '🏠', leftPct: 72, topPct: 74 },
  ]);

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
  trackedHuman = computed(() => {
    const trackedId = this.trackedHumanId();
    if (trackedId === null) {
      return null;
    }
    return this.humans().find((human) => human.id === trackedId) ?? null;
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
  displayedInventions = computed(() =>
    this.inventions().slice(-12).reverse()
  );
  displayedEvents = computed(() =>
    this.events().slice(-20).reverse()
  );

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
  selectedHumanSummary = computed(() => {
    const human = this.selectedHuman();
    if (!human) {
      return null;
    }
    return `${human.name} · ${this.humanBusyLabel(human.busy)}`;
  });
  canSendChat = computed(
    () => !this.chatBusy() && this.chatInput().trim().length > 0
  );

  ngOnInit(): void {
    this.refreshAll();
    this.pollingSubscription = interval(2000).subscribe(() => {
      if (this.isRunning()) {
        this.refreshAll(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
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

  onChatInput(value: string): void {
    this.chatInput.set(value);
  }

  onSendChat(): void {
    const cityId = this.requireCityId();
    const message = this.chatInput().trim();
    if (!cityId || !message || this.chatBusy()) {
      return;
    }

    this.chatBusy.set(true);
    this.chatError.set(null);
    this.chatInput.set('');
    this.chatEntries.update((entries) => [
      ...entries,
      {
        role: 'user',
        content: message,
        timestamp: new Date().toISOString(),
        commandClass: null,
      },
    ]);

    this.cityService
      .sendAgentChat(cityId, {
        message,
        conversationId: this.chatConversationId() ?? undefined,
        selectedHumanId: this.selectedHumanId() ?? undefined,
        selectedEventId: this.selectedEventId() ?? undefined,
        selectedInventionId: this.selectedInventionId() ?? undefined,
      })
      .subscribe({
        next: (response) => {
          this.chatBusy.set(false);
          this.applyChatResponse(response);
        },
        error: (error) => {
          this.chatBusy.set(false);
          this.chatError.set('Agent request failed.');
          console.error('Agent chat request failed:', error);
        },
      });
  }

  selectHuman(human: SimulationSnapshotOutput['humans'][number]): void {
    this.selectedHumanId.set(human.id);
    this.selectedInventionId.set(null);
    this.selectedEventId.set(null);
  }

  selectHumanById(humanId: number): void {
    const human = this.humans().find((item) => item.id === humanId);
    if (human) {
      this.selectHuman(human);
    }
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

  selectEventById(eventId: number): void {
    const event = this.events().find((item) => item.id === eventId);
    if (event) {
      this.selectEvent(event);
    }
  }

  clearSelection(): void {
    this.selectedHumanId.set(null);
    this.selectedInventionId.set(null);
    this.selectedEventId.set(null);
    this.trackedHumanId.set(null);
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

  humanBusyLabel(isBusy: boolean): string {
    return isBusy ? 'Busy' : 'Active';
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
        this.syncBoardEventEntries(timeline.events);
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

  private applyChatResponse(response: AgentChatResponseOutput): void {
    if (response.conversationId) {
      this.chatConversationId.set(response.conversationId);
    }

    this.chatEntries.update((entries) => [
      ...entries,
      {
        role: 'agent',
        content: response.message?.trim() || 'No response message returned.',
        timestamp: new Date().toISOString(),
        commandClass: response.commandClass ?? null,
      },
    ]);

    this.applyUiEffects(response.uiEffects ?? []);
    this.applyGuidedStructuredData(response);
  }

  private applyUiEffects(effects: AgentUiEffectOutput[]): void {
    const resolution = this.agentChatEffectsService.resolve(effects);

    if (resolution.selectedHumanId !== null) {
      this.selectedHumanId.set(resolution.selectedHumanId);
      this.selectedEventId.set(null);
      this.selectedInventionId.set(null);
      this.lastBoardReaction.set(`Focused human ${resolution.selectedHumanId}`);
    }
    if (resolution.trackedHumanId !== null) {
      this.trackedHumanId.set(resolution.trackedHumanId);
      this.lastBoardReaction.set(`Tracking human ${resolution.trackedHumanId}`);
    }
    if (resolution.selectedEventId !== null) {
      this.selectedEventId.set(resolution.selectedEventId);
      this.selectedHumanId.set(null);
      this.selectedInventionId.set(null);
    }
    if (resolution.selectedInventionId !== null) {
      this.selectedInventionId.set(resolution.selectedInventionId);
      this.selectedHumanId.set(null);
      this.selectedEventId.set(null);
    }

    if (resolution.refreshSnapshot) {
      this.boardPulseNonce.update((value) => value + 1);
      this.lastBoardReaction.set('Board refreshed from latest snapshot');
      this.refreshSnapshot(false);
    }
    if (resolution.refreshTimeline) {
      this.boardPulseNonce.update((value) => value + 1);
      this.refreshHistory(false);
    }
    if (resolution.selectedEventId !== null) {
      this.lastBoardReaction.set(`Marked event ${resolution.selectedEventId}`);
    }
    if (resolution.directorInterventionState === 'executed') {
      this.pendingDirectorConfirmation.set(null);
      this.directorBoardState.set('executed');
      this.lastBoardReaction.set('Intervention executed and reflected on board');
    }
    if (resolution.directorInterventionState === 'pending') {
      this.directorBoardState.set('pending');
      this.lastBoardReaction.set('Intervention pending explicit confirmation');
    }
  }

  private applyGuidedStructuredData(response: AgentChatResponseOutput): void {
    const structuredData = this.readStructuredData(response);
    if (!structuredData) {
      return;
    }

    if (structuredData.focusHuman) {
      this.guidedFocus.set(structuredData.focusHuman);
      this.selectedHumanId.set(structuredData.focusHuman.id);
      this.selectedEventId.set(null);
      this.selectedInventionId.set(null);
    }

    if (structuredData.compareHumans) {
      this.guidedComparison.set(structuredData.compareHumans);
      this.selectedHumanId.set(structuredData.compareHumans.left.id);
      this.selectedEventId.set(null);
      this.selectedInventionId.set(null);
    }

    if (structuredData.followHuman) {
      this.guidedFollow.set(structuredData.followHuman);
      this.trackedHumanId.set(structuredData.followHuman.human.id);
      this.selectedHumanId.set(structuredData.followHuman.human.id);
      this.selectedEventId.set(null);
      this.selectedInventionId.set(null);
    }

    if (structuredData.directorConfirmation) {
      this.pendingDirectorConfirmation.set(structuredData.directorConfirmation);
      this.directorBoardState.set('pending');
    }
    if (structuredData.directorIntervention?.status === 'EXECUTED') {
      this.pendingDirectorConfirmation.set(null);
      this.directorBoardState.set('executed');
    }
  }

  private readStructuredData(
    response: AgentChatResponseOutput
  ): GuidedChatStructuredData | null {
    const maybeStructured = (response as AgentChatResponseWithStructuredData)
      .structuredData;
    if (!maybeStructured || typeof maybeStructured !== 'object') {
      return null;
    }
    return maybeStructured;
  }

  onConfirmDirectorIntervention(): void {
    const pending = this.pendingDirectorConfirmation();
    const cityId = this.requireCityId();
    if (!pending || !cityId || this.chatBusy()) {
      return;
    }

    this.chatBusy.set(true);
    this.chatError.set(null);
    const message = `director confirm meet humans`;
    this.chatEntries.update((entries) => [
      ...entries,
      {
        role: 'user',
        content: `Confirm intervention ${pending.commandType}`,
        timestamp: new Date().toISOString(),
        commandClass: 'DIRECTOR',
      },
    ]);

    this.cityService
      .sendAgentChat(
        cityId,
        {
          message,
          conversationId: this.chatConversationId() ?? undefined,
          selectedHumanId: pending.humanIds?.[0],
          confirmationToken: pending.confirmationToken,
          confirmIntervention: true,
        } as AgentChatRequestInput
      )
      .subscribe({
        next: (response) => {
          this.chatBusy.set(false);
          this.applyChatResponse(response);
        },
        error: (error) => {
          this.chatBusy.set(false);
          this.chatError.set('Director confirmation failed.');
          console.error('Director confirmation failed:', error);
        },
      });
  }

  onCancelDirectorIntervention(): void {
    this.pendingDirectorConfirmation.set(null);
    this.directorBoardState.set(null);
  }

  private syncBoardEventEntries(events: EventOutput[]): void {
    const now = Date.now();
    const existing = this.boardEventEntries().filter((entry) => entry.expiresAtMs > now);
    const knownIds = new Set(existing.map((entry) => entry.eventId));
    const fresh = events
      .slice(-16)
      .filter((event) => !knownIds.has(event.id))
      .flatMap((event) => {
        const anchor = event.actorIds?.[0];
        if (typeof anchor !== 'number') {
          return [];
        }
        const isInteraction =
          event.eventType === 'HUMANS_COLLIDED' ||
          event.eventType === 'DIALOGUE_EXCHANGED';
        return [
          {
            eventId: event.id,
            anchorHumanId: anchor,
            expiresAtMs: now + 5000,
            tone: isInteraction ? ('interaction' as const) : ('milestone' as const),
            label: this.formatEnumLabel(event.eventType),
          },
        ];
      });

    this.boardEventEntries.set([...existing, ...fresh].slice(-20));
  }
}

type ChatEntry = {
  role: 'user' | 'agent';
  content: string;
  timestamp: string;
  commandClass: string | null;
};

type GuidedHumanSummary = {
  id: number;
  name: string;
  busy: boolean;
  x: number;
  y: number;
};

type GuidedCompareSummary = {
  left: GuidedHumanSummary;
  right: GuidedHumanSummary;
};

type GuidedFollowEventSummary = {
  id: number;
  tick: number;
  type: string;
  year: number;
};

type GuidedFollowInventionSummary = {
  id: number;
  tickCreated: number;
  title: string;
  category: string;
};

type GuidedFollowSummary = {
  human: GuidedHumanSummary;
  ticks: number;
  fromTick: number;
  resultTick: number;
  eventWindow: GuidedFollowEventSummary[];
  inventionWindow: GuidedFollowInventionSummary[];
};

type GuidedChatStructuredData = {
  focusHuman?: GuidedHumanSummary;
  compareHumans?: GuidedCompareSummary;
  followHuman?: GuidedFollowSummary;
  directorConfirmation?: DirectorConfirmationSummary;
  directorIntervention?: DirectorInterventionSummary;
};

type AgentChatResponseWithStructuredData = AgentChatResponseOutput & {
  structuredData?: GuidedChatStructuredData;
};

type DirectorConfirmationSummary = {
  interventionId: number;
  commandType: string;
  confirmationToken: string;
  expiresAt: string;
  humanIds: number[];
};

type DirectorInterventionSummary = {
  id: number;
  status: string;
  commandType: string;
  humanIds: number[];
  executedTick?: number;
};

type BoardEventEntry = {
  eventId: number;
  anchorHumanId: number;
  expiresAtMs: number;
  tone: 'milestone' | 'interaction';
  label: string;
};
