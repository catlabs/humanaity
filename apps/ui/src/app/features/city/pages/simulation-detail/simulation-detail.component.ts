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
  AgentUiEffectOutput,
  CityOutput,
  EventOutput,
  InventionOutput,
  SimulationCommandOutput,
  SimulationSnapshotOutput,
} from '@api';
import { EventType } from '@shared';
import { Observable, Subscription, interval } from 'rxjs';
import {
  SimulationAssistantBlock,
  SimulationAssistantCommandDescriptor,
  SimulationAssistantResponse,
} from '../../simulation-assistant.models';
import { CityService } from '../../city.service';
import {
  SimulationBoardComponent,
  SimulationBoardEventMarker,
} from '../../components/simulation-board/simulation-board.component';
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
export class SimulationDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private cityService = inject(CityService);
  private agentChatEffectsService = inject(AgentChatEffectsService);
  private boardViewModelService = inject(BoardViewModelService);

  private pollingSubscription?: Subscription;
  private assistantCatalogSubscription?: Subscription;
  private chatEntrySeq = 0;

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
  assistantCommands = signal<SimulationAssistantCommandDescriptor[]>([]);
  assistantSuggestions = computed(() =>
    this.assistantCommands().map((c) => c.canonicalText),
  );
  chatInput = signal('');
  chatBusy = signal(false);
  chatError = signal<string | null>(null);
  chatEntries = signal<ChatEntry[]>([]);
  eventsDrawerOpen = signal(false);
  eventsDrawerType = signal<string | null>(null);
  eventsDrawerIds = signal<number[] | null>(null);
  highlightedPlaceId = signal<string | null>(null);

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
    this.humans().filter((human) => this.filters()[this.humanState(human)]),
  );
  boardMarkers = computed(
    () => this.boardViewModelService.fromHumans(this.humans()).markers,
  );
  boardEventMarkers = computed<SimulationBoardEventMarker[]>(() => {
    const markersByHumanId = new Map(
      this.boardMarkers().map((marker) => [marker.id, marker]),
    );
    return this.events()
      .slice(-16)
      .filter(
        (event) =>
          event.eventType === 'HUMANS_COLLIDED' ||
          event.eventType === 'DIALOGUE_EXCHANGED' ||
          event.eventType === 'DISCOVERY_UNLOCKED',
      )
      .map((event) => {
        const anchors = event.actorIds
          .map((id) => markersByHumanId.get(id))
          .filter((marker): marker is NonNullable<typeof marker> => !!marker);
        if (anchors.length === 0) {
          return null;
        }
        const leftPct =
          anchors.reduce((sum, marker) => sum + marker.leftPct, 0) /
          anchors.length;
        const topPct =
          anchors.reduce((sum, marker) => sum + marker.topPct, 0) /
          anchors.length;
        return {
          eventId: event.id,
          leftPct,
          topPct,
          icon: this.eventMarkerIcon(event),
          kind: this.eventMarkerKind(event),
          label: this.eventTitle(event),
        };
      })
      .filter((entry): entry is SimulationBoardEventMarker => entry !== null);
  });
  boardPlaces = computed<BoardPlaceViewModel[]>(() => [
    { id: 'forest', label: 'Forest', icon: '🌳', leftPct: 14, topPct: 18 },
    { id: 'river', label: 'River', icon: '🌊', leftPct: 82, topPct: 22 },
    { id: 'church', label: 'Church', icon: '⛪', leftPct: 52, topPct: 30 },
    { id: 'campfire', label: 'Campfire', icon: '🔥', leftPct: 34, topPct: 72 },
    { id: 'house', label: 'House', icon: '🏠', leftPct: 72, topPct: 74 },
  ]);

  selectedHuman = computed(() => {
    const id = this.selectedHumanId();
    return id === null
      ? null
      : (this.humans().find((human) => human.id === id) ?? null);
  });

  selectedInvention = computed(() => {
    const id = this.selectedInventionId();
    return id === null
      ? null
      : (this.inventions().find((invention) => invention.id === id) ?? null);
  });

  selectedEvent = computed(() => {
    const id = this.selectedEventId();
    return id === null
      ? null
      : (this.events().find((event) => event.id === id) ?? null);
  });
  trackedHuman = computed(() => {
    const trackedId = this.trackedHumanId();
    if (trackedId === null) {
      return null;
    }
    return this.humans().find((human) => human.id === trackedId) ?? null;
  });

  populationActive = computed(() =>
    Math.max(this.populationTotal() - this.populationBusy(), 0),
  );

  inventionCounts = computed(() => {
    const inventions = this.inventions();
    return {
      scientific: inventions.filter((inv) => inv.category === 'TECHNIQUE')
        .length,
      philosophical: inventions.filter((inv) => inv.category === 'KNOWLEDGE')
        .length,
      cultural: inventions.filter((inv) => inv.category === 'SOCIAL_PRACTICE')
        .length,
      total: inventions.length,
    };
  });
  displayedInventions = computed(() => this.inventions().slice(-12).reverse());
  displayedEvents = computed(() => this.events().slice(-20).reverse());
  drawerEvents = computed(() => {
    const allEvents = this.events();
    const eventIds = this.eventsDrawerIds();
    const eventType = this.eventsDrawerType();
    const byIds =
      Array.isArray(eventIds) && eventIds.length > 0
        ? allEvents.filter((event) => eventIds.includes(event.id))
        : allEvents;
    const byType = eventType
      ? byIds.filter((event) => event.eventType === eventType)
      : byIds;
    return byType.slice(-80).reverse();
  });

  eraLabel = computed(() => this.formatEnumLabel(this.currentEra()));
  phaseLabel = computed(() => this.formatEnumLabel(this.worldPhase()));
  canStep = computed(() => !this.controlBusy() && !this.isRunning());
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
  selectedHumanCoordinates = computed(() => {
    const human = this.selectedHuman();
    if (!human) {
      return null;
    }

    const format = (value: number | null | undefined) =>
      typeof value === 'number' && Number.isFinite(value)
        ? `${Math.round(value * 100)}%`
        : 'n/a';

    return `${format(human.x)} x · ${format(human.y)} y`;
  });
  activityFeed = computed(() => this.displayedEvents().slice(0, 8));
  recentDiscoveries = computed(() => this.displayedInventions().slice(0, 3));
  recentDeltaEventIds = signal<number[]>([]);
  recentDeltaInventionIds = signal<number[]>([]);
  latestTimelineDeltaMessage = signal<string | null>(null);
  private pendingTimelineCommand: SimulationCommandOutput | null = null;
  headerRunSummary = computed(
    () => `Era ${this.eraLabel()} · Year ${this.currentYear()}`,
  );
  storyFocus = computed<StoryFocusCard | null>(() => {
    const selectedEvent = this.selectedEvent();
    if (selectedEvent) {
      return this.storyFocusFromEvent(selectedEvent);
    }

    const selectedInvention = this.selectedInvention();
    if (selectedInvention) {
      return this.storyFocusFromInvention(selectedInvention);
    }

    const latestFreshEvent = this.activityFeed().find((event) =>
      this.isFreshEvent(event.id),
    );
    if (latestFreshEvent) {
      return this.storyFocusFromEvent(latestFreshEvent);
    }

    const latestFreshInvention = this.recentDiscoveries().find((invention) =>
      this.isFreshInvention(invention.id),
    );
    if (latestFreshInvention) {
      return this.storyFocusFromInvention(latestFreshInvention);
    }

    const latestEvent = this.activityFeed()[0];
    if (latestEvent) {
      return this.storyFocusFromEvent(latestEvent);
    }

    const latestInvention = this.recentDiscoveries()[0];
    return latestInvention
      ? this.storyFocusFromInvention(latestInvention)
      : null;
  });
  assistantStatusMessage = computed(() => {
    if (this.chatBusy()) {
      return 'Assistant is reading the simulation…';
    }
    if (this.chatError()) {
      return this.chatError();
    }
    return 'Use a short deterministic command or click a suggestion.';
  });
  assistantStatusIsError = computed(() => !!this.chatError());
  canSendChat = computed(
    () => !this.chatBusy() && this.chatInput().trim().length > 0,
  );

  ngOnInit(): void {
    this.assistantCatalogSubscription = this.cityService
      .getSimulationAssistantCommands()
      .subscribe({
        next: (commands) => {
          this.assistantCommands.set(commands);
          this.chatEntries.update((entries) => {
            if (entries.length === 1 && entries[0].commandType === 'WELCOME') {
              return [this.createAssistantWelcomeEntry()];
            }
            return entries;
          });
        },
        error: () => {
          /* no client-side fallback; select and chips stay empty */
        },
      });
    if (this.chatEntries().length === 0) {
      this.chatEntries.set([this.createAssistantWelcomeEntry()]);
    }
    this.refreshAll();
    this.pollingSubscription = interval(2000).subscribe(() => {
      if (this.isRunning()) {
        this.refreshAll(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.assistantCatalogSubscription?.unsubscribe();
    this.pollingSubscription?.unsubscribe();
  }

  toggleFilter(state: string): void {
    this.filters.update((prev) => ({ ...prev, [state]: !prev[state] }));
  }

  onStep(): void {
    if (!this.canStep()) {
      return;
    }
    this.submitSimulationCommand('advance 1');
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
    const commandText = this.chatInput().trim();
    if (!commandText) {
      return;
    }
    this.submitAssistantCommand(commandText);
  }

  onAssistantSuggestion(commandText: string): void {
    if (this.chatBusy()) {
      return;
    }
    this.submitAssistantCommand(commandText);
  }

  onAssistantCommandSelect(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const value = select.value.trim();
    select.value = '';
    if (!value || this.chatBusy()) {
      return;
    }
    this.submitAssistantCommand(value);
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
    this.selectedInventionId.set(null);

    const primaryActorId = event.actorIds.find((actorId) =>
      this.humans().some((human) => human.id === actorId),
    );
    this.selectedHumanId.set(primaryActorId ?? null);
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

  closeEventsDrawer(): void {
    this.eventsDrawerOpen.set(false);
  }

  openAllEventsDrawer(): void {
    this.eventsDrawerType.set(null);
    this.eventsDrawerIds.set(null);
    this.eventsDrawerOpen.set(true);
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

  eventTypeLabel(value: string): string {
    return this.formatEnumLabel(value);
  }

  eventDescription(event: EventOutput): string {
    return `Tick ${event.tick} • Year ${event.year} • ${this.formatEnumLabel(event.era)}`;
  }

  eventCanonicalSummary(event: EventOutput): string {
    return `${this.eventDescription(event)} • ${this.formatEnumLabel(event.eventCategory)}`;
  }

  eventActorSummary(event: EventOutput): string | null {
    if (!Array.isArray(event.actorIds) || event.actorIds.length === 0) {
      return null;
    }

    const names = event.actorIds
      .map(
        (actorId) => this.humans().find((human) => human.id === actorId)?.name,
      )
      .filter((name): name is string => !!name);

    if (names.length === 0) {
      return null;
    }

    return names.join(' · ');
  }

  eventNarrative(event: EventOutput): string | null {
    return event.enrichedSnippet?.trim() || null;
  }

  eventNarrationLabel(event: EventOutput): string {
    switch (event.enrichmentStatus) {
      case 'READY':
        return 'AI narration ready';
      case 'FALLBACK':
        return 'Fallback narration';
      default:
        return this.eventSupportsNarration(event)
          ? 'Narration pending'
          : 'No narration target';
    }
  }

  eventNarrationCopy(event: EventOutput): string {
    const narrative = this.eventNarrative(event);
    if (narrative) {
      return narrative;
    }
    return this.eventSupportsNarration(event)
      ? 'AI narration is not available for this event yet.'
      : 'This event type stays canonical-only and does not currently receive narration.';
  }

  eventNarrationTone(event: EventOutput): 'ready' | 'fallback' | 'muted' {
    switch (event.enrichmentStatus) {
      case 'READY':
        return 'ready';
      case 'FALLBACK':
        return 'fallback';
      default:
        return 'muted';
    }
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

  inventionNarratedTitle(invention: InventionOutput): string | null {
    const narratedTitle = invention.enrichedTitle?.trim();
    if (!narratedTitle || narratedTitle === invention.title) {
      return null;
    }
    return narratedTitle;
  }

  inventionNarrationLabel(invention: InventionOutput): string {
    switch (invention.enrichmentStatus) {
      case 'READY':
        return 'AI narration ready';
      case 'FALLBACK':
        return 'Fallback narration';
      default:
        return 'Narration pending';
    }
  }

  inventionNarrationCopy(invention: InventionOutput): string {
    const narrative = invention.enrichedSummary?.trim();
    if (narrative) {
      return narrative;
    }
    return 'AI narration is not available for this discovery yet.';
  }

  inventionNarrationTone(
    invention: InventionOutput,
  ): 'ready' | 'fallback' | 'muted' {
    switch (invention.enrichmentStatus) {
      case 'READY':
        return 'ready';
      case 'FALLBACK':
        return 'fallback';
      default:
        return 'muted';
    }
  }

  inventionEnrichmentStatusLabel(invention: InventionOutput): string {
    return this.formatEnumLabel(invention.enrichmentStatus);
  }

  humanBusyLabel(isBusy: boolean): string {
    return isBusy ? 'Busy' : 'Active';
  }

  isFreshEvent(eventId: number): boolean {
    return this.recentDeltaEventIds().includes(eventId);
  }

  isFreshInvention(inventionId: number): boolean {
    return this.recentDeltaInventionIds().includes(inventionId);
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
    const previousEvents = this.events();
    const previousInventions = this.inventions();

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
        this.captureTimelineDelta(
          previousEvents,
          previousInventions,
          timeline.events,
          timeline.inventions,
          showLoading,
        );
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
      snapshot.run.status ?? (snapshot.run.running ? 'RUNNING' : 'CREATED'),
    );

    this.populationTotal.set(snapshot.metrics.population);
    this.populationBusy.set(snapshot.metrics.busyCount);
    this.totalEvents.set(snapshot.metrics.eventCount);
    this.totalInventions.set(snapshot.metrics.inventionCount);
    this.recentEventCount.set(snapshot.timelineSummary.recentEventCount);
    this.recentInventionCount.set(
      snapshot.timelineSummary.recentInventionCount,
    );
    this.humans.set(snapshot.humans);

    if (this.events().length === 0 && snapshot.recentEvents.length > 0) {
      this.events.set(snapshot.recentEvents);
    }
    if (
      this.inventions().length === 0 &&
      snapshot.recentInventions.length > 0
    ) {
      this.inventions.set(snapshot.recentInventions);
    }
  }

  private submitSimulationCommand(commandText: string): void {
    const cityId = this.requireCityId();
    if (!cityId || this.chatBusy()) {
      return;
    }

    this.chatBusy.set(true);
    this.chatError.set(null);

    this.cityService.sendSimulationCommand(cityId, { commandText }).subscribe({
      next: (response) => {
        this.chatBusy.set(false);
        if (response.ok) {
          this.pendingTimelineCommand = this.hasTimelineRefreshEffect(response)
            ? response
            : null;
          this.applyUiEffects(response.uiEffects ?? []);
        } else {
          this.chatError.set(response.message ?? 'Simulation command failed.');
        }
      },
      error: (error) => {
        this.chatBusy.set(false);
        this.pendingTimelineCommand = null;
        this.chatError.set('Simulation command request failed.');
        console.error('Simulation command request failed:', error);
      },
    });
  }

  private submitAssistantCommand(commandText: string): void {
    const cityId = this.requireCityId();
    if (!cityId || this.chatBusy()) {
      return;
    }

    const userEntry = this.createUserEntry(commandText);
    this.chatEntries.update((entries) => [...entries, userEntry]);
    this.chatBusy.set(true);
    this.chatError.set(null);
    this.chatInput.set('');

    this.cityService
      .sendSimulationAssistantCommand(cityId, commandText)
      .subscribe({
        next: (response) => {
          this.chatBusy.set(false);
          this.chatEntries.update((entries) => [
            ...entries,
            this.createAssistantEntry(response),
          ]);
        },
        error: (error) => {
          this.chatBusy.set(false);
          this.chatError.set('Assistant request failed.');
          this.chatEntries.update((entries) => [
            ...entries,
            this.createAssistantErrorEntry(),
          ]);
          console.error('Simulation assistant request failed:', error);
        },
      });
  }

  private hasTimelineRefreshEffect(response: SimulationCommandOutput): boolean {
    return (response.uiEffects ?? []).some(
      (effect) => effect.type === 'REFRESH_TIMELINE',
    );
  }

  private captureTimelineDelta(
    previousEvents: EventOutput[],
    previousInventions: InventionOutput[],
    nextEvents: EventOutput[],
    nextInventions: InventionOutput[],
    showLoading: boolean,
  ): void {
    if (showLoading) {
      return;
    }

    const previousEventIds = new Set(previousEvents.map((event) => event.id));
    const previousInventionIds = new Set(
      previousInventions.map((invention) => invention.id),
    );

    const newEvents = nextEvents.filter(
      (event) => !previousEventIds.has(event.id),
    );
    const newInventions = nextInventions.filter(
      (invention) => !previousInventionIds.has(invention.id),
    );

    if (newEvents.length === 0 && newInventions.length === 0) {
      this.recentDeltaEventIds.set([]);
      this.recentDeltaInventionIds.set([]);
      if (this.pendingTimelineCommand) {
        this.latestTimelineDeltaMessage.set(
          this.pendingTimelineCommand.message ?? null,
        );
      }
      this.pendingTimelineCommand = null;
      return;
    }

    this.recentDeltaEventIds.set(newEvents.slice(-6).map((event) => event.id));
    this.recentDeltaInventionIds.set(
      newInventions.slice(-3).map((invention) => invention.id),
    );

    const newestEvent = newEvents[newEvents.length - 1];
    if (newestEvent) {
      this.selectEvent(newestEvent);
    }

    this.latestTimelineDeltaMessage.set(
      this.summarizeTimelineDelta(newEvents.length, newInventions.length),
    );
    this.pendingTimelineCommand = null;
  }

  private summarizeTimelineDelta(
    eventCount: number,
    inventionCount: number,
  ): string {
    const parts: string[] = [];

    if (eventCount > 0) {
      parts.push(`${eventCount} new ${eventCount === 1 ? 'event' : 'events'}`);
    }
    if (inventionCount > 0) {
      parts.push(
        `${inventionCount} new ${inventionCount === 1 ? 'discovery' : 'discoveries'}`,
      );
    }

    const deltaLabel = parts.join(' and ');
    const commandMessage = this.pendingTimelineCommand?.message?.trim();
    if (commandMessage) {
      return `${commandMessage} · ${deltaLabel}`;
    }
    return `Live timeline updated with ${deltaLabel}.`;
  }

  private eventSupportsNarration(event: EventOutput): boolean {
    return (
      event.eventCategory === 'DIALOGUE' ||
      event.eventType === 'DIALOGUE_EXCHANGED'
    );
  }

  private storyFocusFromEvent(event: EventOutput): StoryFocusCard {
    return {
      kind: 'event',
      label: 'Event',
      title: this.eventTitle(event),
      meta: this.eventCanonicalSummary(event),
      actorSummary: this.eventActorSummary(event),
      narrationLabel: this.eventNarrationLabel(event),
      narrationTone: this.eventNarrationTone(event),
      narrationCopy: this.eventNarrationCopy(event),
    };
  }

  private storyFocusFromInvention(invention: InventionOutput): StoryFocusCard {
    return {
      kind: 'discovery',
      label: 'Discovery',
      title: this.inventionNarratedTitle(invention) ?? invention.title,
      meta: `${this.inventionCategoryLabel(invention)} • Impact ${invention.impactScore} • Year ${invention.yearCreated}`,
      actorSummary: invention.title,
      narrationLabel: this.inventionNarrationLabel(invention),
      narrationTone: this.inventionNarrationTone(invention),
      narrationCopy: this.inventionNarrationCopy(invention),
    };
  }

  private runControlAction(
    action: (cityId: number) => Observable<unknown>,
  ): void {
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

  private applyUiEffects(effects: AgentUiEffectOutput[]): void {
    const resolution = this.agentChatEffectsService.resolve(effects);

    if (resolution.selectedHumanId !== null) {
      this.selectedHumanId.set(resolution.selectedHumanId);
      this.selectedEventId.set(null);
      this.selectedInventionId.set(null);
    }
    if (resolution.trackedHumanId !== null) {
      this.trackedHumanId.set(resolution.trackedHumanId);
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
      this.refreshSnapshot(false);
    }
    if (resolution.refreshTimeline) {
      this.refreshHistory(false);
    }
    if (resolution.openEventsDrawer) {
      this.eventsDrawerOpen.set(true);
      this.eventsDrawerType.set(resolution.drawerEventType);
      this.eventsDrawerIds.set(resolution.drawerEventIds);
    }
    if (resolution.highlightedPlaceId !== null) {
      this.highlightedPlaceId.set(resolution.highlightedPlaceId);
    }
  }

  private nextChatEntryId(): string {
    return `chat-${++this.chatEntrySeq}`;
  }

  private createAssistantWelcomeEntry(): ChatEntry {
    return {
      id: this.nextChatEntryId(),
      role: 'assistant',
      content:
        'Ask for a deterministic read of the simulation with one of the supported commands.',
      timestamp: new Date().toISOString(),
      commandType: 'WELCOME',
      blocks: [
        {
          type: 'SUPPORTED_COMMANDS',
          title: 'Quick suggestions',
          subtitle: 'Start with one of these backend-supported reads.',
          metrics: [],
          items: this.assistantCommands().map((command) => ({
            title: command.canonicalText,
            subtitle: command.label,
            body: command.description,
            chips: [],
          })),
          emptyState: null,
        },
      ],
    };
  }

  private createUserEntry(commandText: string): ChatEntry {
    return {
      id: this.nextChatEntryId(),
      role: 'user',
      content: commandText,
      timestamp: new Date().toISOString(),
      commandType: null,
      blocks: [],
    };
  }

  private createAssistantEntry(
    response: SimulationAssistantResponse,
  ): ChatEntry {
    return {
      id: this.nextChatEntryId(),
      role: 'assistant',
      content: response.text?.trim() || 'No assistant text returned.',
      timestamp: new Date().toISOString(),
      commandType: response.commandType ?? null,
      blocks: response.blocks ?? [],
    };
  }

  private createAssistantErrorEntry(): ChatEntry {
    return {
      id: this.nextChatEntryId(),
      role: 'assistant',
      content:
        'The assistant could not load a deterministic response right now.',
      timestamp: new Date().toISOString(),
      commandType: 'ERROR',
      blocks: [],
    };
  }

  private eventMarkerKind(
    event: EventOutput,
  ): SimulationBoardEventMarker['kind'] {
    switch (event.eventType) {
      case 'HUMANS_COLLIDED':
        return 'collision';
      case 'DIALOGUE_EXCHANGED':
        return 'dialogue';
      default:
        return 'discovery';
    }
  }

  private eventMarkerIcon(event: EventOutput): string {
    if (event.eventType === 'HUMANS_COLLIDED') {
      return '✦';
    }
    if (event.eventType === 'DIALOGUE_EXCHANGED') {
      return '💬';
    }
    const category = event.payload?.['inventionCategory'];
    switch (category) {
      case 'TECHNIQUE':
        return '⚙';
      case 'SOCIAL_PRACTICE':
        return '🏛';
      case 'KNOWLEDGE':
        return '📜';
      default:
        return '📜';
    }
  }
}

type ChatEntry = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  commandType: string | null;
  blocks: SimulationAssistantBlock[];
};

type StoryFocusCard = {
  kind: 'event' | 'discovery';
  label: string;
  title: string;
  meta: string;
  actorSummary: string | null;
  narrationLabel: string;
  narrationTone: 'ready' | 'fallback' | 'muted';
  narrationCopy: string;
};
