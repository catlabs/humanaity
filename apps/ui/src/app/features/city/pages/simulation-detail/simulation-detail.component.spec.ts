import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CityService } from '../../city.service';
import type { SimulationAssistantCommandDescriptor } from '../../simulation-assistant.models';

const testAssistantCatalog: SimulationAssistantCommandDescriptor[] = [
  {
    commandType: 'INVENTIONS',
    canonicalText: 'inventions',
    label: 'Inventions',
    description: 'Latest unlocked inventions.',
  },
  {
    commandType: 'WORLD_STATUS',
    canonicalText: 'world status',
    label: 'World status',
    description: 'Run snapshot.',
  },
  {
    commandType: 'RECENT_EVENTS',
    canonicalText: 'recent events',
    label: 'Recent events',
    description: 'Recent events.',
  },
  {
    commandType: 'RELATIONSHIPS',
    canonicalText: 'relationships',
    label: 'Relationships',
    description: 'Pairs.',
  },
];
import { SimulationDetailComponent } from './simulation-detail.component';

describe('SimulationDetailComponent', () => {
  let fixture: ComponentFixture<SimulationDetailComponent>;
  let cityService: jasmine.SpyObj<CityService>;

  const snapshot = {
    city: { id: 7, name: 'Spec City' },
    run: {
      hasRun: false,
      running: false,
      tick: 0,
      year: 1,
      era: 'FOUNDING',
      status: 'CREATED',
    },
    timelineSummary: {
      recentEventCount: 0,
      recentInventionCount: 0,
    },
    humans: [],
    metrics: {
      population: 0,
      busyCount: 0,
      busyRatio: 0,
      eventCount: 0,
      inventionCount: 0,
    },
    recentEvents: [
      {
        id: 12,
        cityId: 7,
        tick: 4,
        sequenceInTick: 1,
        eventCategory: 'DIALOGUE',
        eventType: 'DIALOGUE_EXCHANGED',
        actorIds: [1, 2],
        payload: {},
        importance: 1,
        year: 1,
        era: 'FOUNDING',
        eventKey: 'DIALOGUE_EXCHANGED:4:1',
        createdAt: '2026-01-01T00:00:00Z',
        enrichmentStatus: 'READY',
        enrichmentFallback: false,
        enrichedSnippet: 'Two neighbors agreed on a shared water route.',
      },
    ],
    recentInventions: [
      {
        id: 34,
        cityId: 7,
        tickCreated: 4,
        category: 'TECHNIQUE',
        inventionKey: 'DISCOVERY:4:1',
        title: 'Canal Layout',
        summary: 'Basic canal planning pattern.',
        sourceEventKeys: ['DISCOVERY_UNLOCKED:4:1'],
        impactScore: 42,
        yearCreated: 1,
        eraCreated: 'FOUNDING',
        createdAt: '2026-01-01T00:00:00Z',
        enrichmentStatus: 'FALLBACK',
        enrichmentFallback: true,
        enrichedTitle: 'Canal Layout (Field Note)',
        enrichedSummary: 'Fallback: Basic canal planning pattern.',
      },
    ],
  } as any;

  const timeline = {
    cityId: 7,
    fromTick: 0,
    eventCount: 0,
    inventionCount: 0,
    events: snapshot.recentEvents,
    inventions: snapshot.recentInventions,
  } as any;

  beforeEach(async () => {
    cityService = jasmine.createSpyObj<CityService>('CityService', [
      'getSimulationSnapshot',
      'getSimulationTimeline',
      'startSimulation',
      'stopSimulation',
      'stepSimulation',
      'sendSimulationCommand',
      'sendSimulationAssistantCommand',
      'getSimulationAssistantCommands',
    ]);

    cityService.getSimulationSnapshot.and.returnValue(of(snapshot));
    cityService.getSimulationTimeline.and.returnValue(of(timeline));
    cityService.startSimulation.and.returnValue(of(void 0));
    cityService.stopSimulation.and.returnValue(of(void 0));
    cityService.stepSimulation.and.returnValue(of(void 0));
    cityService.sendSimulationCommand.and.returnValue(
      of({
        ok: true,
        commandType: 'ADVANCE',
        message: 'Advanced city by 3 steps.',
        mutated: true,
        referencedEntities: { humanId: null, placeId: null },
        uiEffects: [
          { type: 'REFRESH_SNAPSHOT' },
          { type: 'REFRESH_TIMELINE' },
        ],
      } as any)
    );
    cityService.sendSimulationAssistantCommand.and.returnValue(
      of({
        ok: true,
        text: 'World status summary.',
        commandType: 'WORLD_STATUS',
        blocks: [],
      } as any)
    );
    cityService.getSimulationAssistantCommands.and.returnValue(
      of(testAssistantCatalog),
    );

    await TestBed.configureTestingModule({
      imports: [SimulationDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { city: { id: 7, name: 'Spec City' } } } },
        },
        { provide: CityService, useValue: cityService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SimulationDetailComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('loads snapshot and history on init', () => {
    expect(cityService.getSimulationSnapshot).toHaveBeenCalledWith(7);
    expect(cityService.getSimulationTimeline).toHaveBeenCalledWith(7, 100);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Spec City');
    expect(text).toContain('Era Founding · Year 1');
    expect(text).toContain('Use a short deterministic command or click a suggestion.');
    expect(text).toContain('readable board state');
    expect(text).toContain('Story focus');
    expect(text).toContain('Timeline');
    expect(text).toContain('No simulation run yet');
    expect(text).toContain('AI narration ready');
    expect(text).toContain('Fallback narration');
    expect(fixture.nativeElement.querySelector('app-simulation-board')).not.toBeNull();
  });

  it('submits advance 1 through the deterministic command endpoint from the step button', () => {
    fixture.componentInstance.onStep();

    expect(cityService.sendSimulationCommand).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ commandText: 'advance 1' })
    );
  });

  it('submits assistant command requests and stores the backend reply in chat entries', () => {
    const component = fixture.componentInstance;
    component.onChatInput('world status');
    component.onSendChat();
    fixture.detectChanges();

    expect(cityService.sendSimulationAssistantCommand).toHaveBeenCalledWith(
      7,
      'world status'
    );

    const entries = component.chatEntries();
    const assistantReply = entries.find((e) => e.role === 'assistant' && e.commandType === 'WORLD_STATUS');
    expect(assistantReply?.content).toContain('World status summary.');
  });

  it('applies uiEffects from deterministic command responses', () => {
    cityService.sendSimulationCommand.and.returnValue(
      of({
        ok: true,
        commandType: 'MOVE_HUMAN_TO_PLACE',
        message: 'Assigned Ada to move toward forest.',
        mutated: true,
        referencedEntities: { humanId: 12, placeId: 'forest' },
        uiEffects: [
          { type: 'FOCUS_HUMAN', humanId: 12 },
          { type: 'HIGHLIGHT_PLACE', placeId: 'forest' },
        ],
      } as any)
    );

    fixture.componentInstance.onStep();
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedHumanId()).toBe(12);
    expect(fixture.componentInstance.highlightedPlaceId()).toBe('forest');
  });

  it('keeps the primary actor focused when selecting an event', () => {
    fixture.componentInstance.humans.set([
      { id: 1, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
      { id: 2, name: 'Ben', busy: true, x: 0.3, y: 0.4 },
    ] as any);

    fixture.componentInstance.selectEvent(snapshot.recentEvents[0] as any);

    expect(fixture.componentInstance.selectedEventId()).toBe(12);
    expect(fixture.componentInstance.selectedHumanId()).toBe(1);
  });

  it('surfaces the latest timeline delta after a command-triggered refresh', async () => {
    const refreshedSnapshot = {
      ...snapshot,
      run: {
        ...snapshot.run,
        hasRun: true,
        tick: 1,
      },
      metrics: {
        ...snapshot.metrics,
        eventCount: 2,
      },
      timelineSummary: {
        recentEventCount: 2,
        recentInventionCount: 0,
      },
    } as any;
    const newEvent = {
      id: 99,
      cityId: 7,
      tick: 5,
      sequenceInTick: 1,
      eventCategory: 'MOVEMENT',
      eventType: 'HUMANS_COLLIDED',
      actorIds: [1],
      payload: {},
      importance: 1,
      year: 1,
      era: 'FOUNDING',
      eventKey: 'HUMANS_COLLIDED:5:1',
      createdAt: '2026-01-01T00:00:01Z',
      enrichmentStatus: 'READY',
      enrichmentFallback: false,
      enrichedSnippet: 'Ada collided with another traveler.',
    } as any;
    const refreshedTimeline = {
      ...timeline,
      eventCount: 2,
      events: [...snapshot.recentEvents, newEvent],
    } as any;

    cityService.getSimulationSnapshot.and.returnValues(of(snapshot), of(refreshedSnapshot));
    cityService.getSimulationTimeline.and.returnValues(of(timeline), of(refreshedTimeline));
    fixture = TestBed.createComponent(SimulationDetailComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.humans.set([
      { id: 1, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
    ] as any);

    fixture.componentInstance.onStep();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.latestTimelineDeltaMessage()).toContain(
      '1 new event'
    );
    expect(fixture.componentInstance.selectedEventId()).toBe(99);
    expect(fixture.componentInstance.isFreshEvent(99)).toBeTrue();
    expect(fixture.nativeElement.textContent as string).toContain('Latest delta');
  });

  it('renders explicit narration states for canonical and fallback history items', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Tick 4 • Year 1 • Founding • Dialogue');
    expect(text).toContain('AI narration ready');
    expect(text).toContain('Two neighbors agreed on a shared water route.');
    expect(text).toContain('Fallback narration');
    expect(text).toContain('Impact 42');
    expect(text).toContain('Basic canal planning pattern.');
  });

  it('surfaces the selected event in the story focus panel', () => {
    fixture.componentInstance.humans.set([
      { id: 1, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
      { id: 2, name: 'Ben', busy: true, x: 0.3, y: 0.4 },
    ] as any);

    fixture.componentInstance.selectEvent(snapshot.recentEvents[0] as any);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Story focus');
    expect(text).toContain('Event');
    expect(text).toContain('Dialogue Exchanged');
    expect(text).toContain('Two neighbors agreed on a shared water route.');
  });

  it('returns explicit empty-state narration copy when enrichment is unavailable', () => {
    const component = fixture.componentInstance;
    const unsupportedEvent = {
      ...snapshot.recentEvents[0],
      eventCategory: 'INTERACTION',
      eventType: 'HUMANS_COLLIDED',
      enrichmentStatus: 'NONE',
      enrichmentFallback: false,
      enrichedSnippet: undefined,
    } as any;
    const uninrichedInvention = {
      ...snapshot.recentInventions[0],
      enrichmentStatus: 'NONE',
      enrichmentFallback: false,
      enrichedTitle: undefined,
      enrichedSummary: undefined,
    } as any;

    expect(component.eventNarrationLabel(unsupportedEvent)).toBe('No narration target');
    expect(component.eventNarrationCopy(unsupportedEvent)).toContain(
      'does not currently receive narration'
    );
    expect(component.inventionNarrationLabel(uninrichedInvention)).toBe(
      'Narration pending'
    );
    expect(component.inventionNarrationCopy(uninrichedInvention)).toContain(
      'not available for this discovery yet'
    );
  });

  it('renders assistant request failures clearly', () => {
    cityService.sendSimulationAssistantCommand.and.returnValue(
      throwError(() => new Error('Unsupported command.'))
    );

    fixture.componentInstance.onChatInput('world status');
    fixture.componentInstance.onSendChat();
    fixture.detectChanges();

    expect(fixture.componentInstance.assistantStatusIsError()).toBeTrue();
    expect((fixture.nativeElement.textContent as string)).toContain(
      'Assistant request failed.'
    );
  });
});
