import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { CityService } from '../../city.service';
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
      'sendAgentChat',
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
    cityService.sendAgentChat.and.returnValue(of({} as any));

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
    expect(text).toContain('Selected human');
    expect(text).toContain('Recent activity');
    expect(text).toContain('Command console');
    expect(text).toContain('No simulation run yet');
    expect(fixture.nativeElement.querySelector('app-simulation-board')).not.toBeNull();
  });

  it('submits advance 1 through the deterministic command endpoint from the step button', () => {
    fixture.componentInstance.onStep();

    expect(cityService.sendSimulationCommand).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ commandText: 'advance 1' })
    );
  });

  it('submits deterministic command requests and renders the backend reply', () => {
    const component = fixture.componentInstance;
    component.onChatInput('advance 3');
    component.onSendChat();
    fixture.detectChanges();

    expect(cityService.sendSimulationCommand).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ commandText: 'advance 3' })
    );

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Command console');
    expect(text).toContain('Advanced city by 3 steps.');
    expect(text).toContain('Advance');
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

    fixture.componentInstance.onChatInput('move 12 forest');
    fixture.componentInstance.onSendChat();
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

  it('renders rejected deterministic commands clearly', () => {
    cityService.sendSimulationCommand.and.returnValue(
      of({
        ok: false,
        commandType: 'UNSUPPORTED',
        message:
          'Unsupported command. Use `advance <count>`, `focus <human>`, or `move <human> <place>`.',
        mutated: false,
        referencedEntities: { humanId: null, placeId: null },
        uiEffects: [],
      } as any)
    );

    fixture.componentInstance.onChatInput('advance by 3 steps');
    fixture.componentInstance.onSendChat();
    fixture.detectChanges();

    expect(fixture.componentInstance.commandConsoleHasError()).toBeTrue();
    expect((fixture.nativeElement.textContent as string)).toContain(
      'Unsupported command.'
    );
  });
});
