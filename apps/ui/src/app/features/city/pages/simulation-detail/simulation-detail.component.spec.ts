import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { CityService } from '../../city.service';
import { SimulationDetailComponent } from './simulation-detail.component';

describe('SimulationDetailComponent', () => {
  let fixture: ComponentFixture<SimulationDetailComponent>;
  let component: SimulationDetailComponent;
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
    humans: [
      { id: 1, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
      { id: 2, name: 'Ben', busy: true, x: 0.3, y: 0.4 },
    ],
    metrics: {
      population: 2,
      busyCount: 1,
      busyRatio: 0.5,
      eventCount: 0,
      inventionCount: 0,
    },
    recentEvents: [],
    recentInventions: [],
  } as any;

  const timeline = {
    cityId: 7,
    fromTick: 0,
    eventCount: 0,
    inventionCount: 0,
    events: [],
    inventions: [],
  } as any;

  const timelineWithDelta = {
    cityId: 7,
    fromTick: 4,
    eventCount: 1,
    inventionCount: 1,
    events: [
      {
        id: 100,
        eventType: 'HUMAN_ACTION_PERFORMED',
        eventCategory: 'ACTION',
        actorIds: [1],
        tick: 5,
        year: 1,
        era: 'FOUNDING',
        enrichmentStatus: 'PENDING',
        enrichedSnippet: null,
      },
    ],
    inventions: [
      {
        id: 200,
        title: 'Campfire Rhythm',
        summary: 'Coordination improves.',
        category: 'SOCIAL_PRACTICE',
        impactScore: 3,
        yearCreated: 1,
        enrichmentStatus: 'PENDING',
      },
    ],
  } as any;

  const commandBuilder = {
    actorOptions: [
      { value: '1', label: 'Ada' },
      { value: '2', label: 'Ben' },
    ],
    actions: [
      {
        actionKey: 'WORLD_STATUS',
        label: 'World status',
        executionKind: 'QUERY',
        actorKind: 'NONE',
        targetKind: 'NONE',
        commandText: 'world status',
        targetOptions: [],
      },
      {
        actionKey: 'MOVE_HUMAN_TO_PLACE',
        label: 'Go to place',
        executionKind: 'COMMAND',
        actorKind: 'HUMAN',
        targetKind: 'PLACE',
        commandVerb: 'move',
        targetOptions: [{ value: 'forest', label: 'Forest' }],
      },
      {
        actionKey: 'MEET_HUMAN',
        label: 'Meet human',
        executionKind: 'COMMAND',
        actorKind: 'HUMAN',
        targetKind: 'HUMAN',
        commandVerb: 'meet',
        requiresDifferentTarget: true,
        targetOptions: [
          { value: '1', label: 'Ada' },
          { value: '2', label: 'Ben' },
        ],
      },
    ],
  } as any;

  beforeEach(async () => {
    cityService = jasmine.createSpyObj<CityService>('CityService', [
      'getSimulationSnapshot',
      'getSimulationTimeline',
      'sendSimulationCommand',
      'sendSimulationAssistantCommand',
      'getSimulationCommandBuilder',
      'startSimulation',
      'stopSimulation',
    ]);

    cityService.getSimulationSnapshot.and.returnValue(of(snapshot));
    cityService.getSimulationTimeline.and.returnValue(of(timeline));
    cityService.getSimulationCommandBuilder.and.returnValue(of(commandBuilder));
    cityService.startSimulation.and.returnValue(of(void 0));
    cityService.stopSimulation.and.returnValue(of(void 0));
    cityService.sendSimulationCommand.and.returnValue(
      of({
        ok: true,
        commandType: 'ADVANCE',
        message: 'Advanced city by 1 step.',
        mutated: true,
        referencedEntities: {
          humanId: null,
          placeId: null,
          targetHumanId: null,
        },
        uiEffects: [{ type: 'REFRESH_SNAPSHOT' }, { type: 'REFRESH_TIMELINE' }],
      } as any),
    );
    cityService.sendSimulationAssistantCommand.and.returnValue(
      of({
        ok: true,
        text: 'World status summary.',
        commandType: 'WORLD_STATUS',
        blocks: [],
      } as any),
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
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  async function flushView(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    await Promise.resolve();
    fixture.detectChanges();
  }

  it('loads snapshot history and command-builder metadata on init without empty placeholder copy', () => {
    expect(cityService.getSimulationSnapshot).toHaveBeenCalledWith(7);
    expect(cityService.getSimulationTimeline).toHaveBeenCalledWith(7, 100);
    expect(cityService.getSimulationCommandBuilder).toHaveBeenCalledWith(7);

    const native = fixture.nativeElement as HTMLElement;
    const text = native.textContent as string;

    expect(text).toContain('Spec City');
    expect(text).not.toContain('Command builder');
    expect(text).not.toContain('No revealed insights yet.');
    expect(native.querySelectorAll('.chat-turn').length).toBe(0);
  });

  it('executes actorless query action and appends assistant turns in order', async () => {
    component.builderActionKey.set('WORLD_STATUS');

    component.onExecuteBuilderAction();
    component.onExecuteBuilderAction();
    await flushView();

    expect(cityService.sendSimulationAssistantCommand).toHaveBeenCalledWith(
      7,
      'world status',
    );
    expect(component.chatTurns().length).toBe(2);
    expect(component.chatTurns().map((turn) => turn.kind)).toEqual([
      'assistant',
      'assistant',
    ]);
    expect(component.chatTurns()[0]?.contextSummary).toBe('World status');
    expect(component.chatTurns()[1]?.message).toBe('World status summary.');
  });

  it('builds and submits deterministic command text for actor+target actions', () => {
    component.builderActionKey.set('MOVE_HUMAN_TO_PLACE');
    component.builderActorValue.set('1');
    component.builderTargetValue.set('forest');

    component.onExecuteBuilderAction();

    expect(cityService.sendSimulationCommand).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ commandText: 'move 1 forest' }),
    );
    expect(component.chatTurns().length).toBe(1);
    expect(component.chatTurns()[0]?.kind).toBe('command');
    expect(component.chatTurns()[0]?.contextSummary).toBe(
      'Go to place · Ada · Forest',
    );
  });

  it('merges timeline and discoveries into the newest command turn', async () => {
    cityService.getSimulationTimeline.and.returnValue(of(timelineWithDelta));
    component.builderActionKey.set('MOVE_HUMAN_TO_PLACE');
    component.builderActorValue.set('1');
    component.builderTargetValue.set('forest');

    component.onExecuteBuilderAction();
    await flushView();

    expect(component.chatTurns().length).toBe(1);
    expect(component.chatTurns()[0]?.message).toBe('Advanced city by 1 step.');
    expect(component.chatTurns()[0]?.timelineEvents.length).toBe(1);
    expect(component.chatTurns()[0]?.discoveryInventions.length).toBe(1);
    expect(component.chatTurns()[0]?.deltaMessage).toBe(
      'Timeline updated with 1 new event and 1 new discovery.',
    );

    const text = (fixture.nativeElement as HTMLElement).textContent as string;
    expect(text).toContain('Campfire Rhythm');
    expect(text).toContain('Recent events');
    expect(text).toContain('Recent discoveries');
  });

  it('prevents submission for invalid combinations', () => {
    component.builderActionKey.set('MOVE_HUMAN_TO_PLACE');
    component.builderActorValue.set('');
    component.builderTargetValue.set('forest');

    expect(component.canExecuteBuilderAction()).toBeFalse();
  });

  it('keeps legacy removed UI out of the template', () => {
    const native = fixture.nativeElement as HTMLElement;
    expect(native.querySelector('.assistant-suggestions')).toBeNull();
    expect(native.querySelector('.console-feedback')).toBeNull();
    expect(native.querySelector('.chat-row')).toBeNull();
    expect(native.querySelector('.insight-card')).toBeNull();
    expect(native.querySelector('.insight-placeholder')).toBeNull();
  });

  it('appends story turns when selecting events and discoveries', async () => {
    component.events.set([
      {
        id: 42,
        eventType: 'DIALOGUE_EXCHANGED',
        eventCategory: 'DIALOGUE',
        actorIds: [1],
        tick: 3,
        year: 1,
        era: 'FOUNDING',
        enrichmentStatus: 'READY',
        enrichedSnippet: 'Ada and Ben exchanged ideas.',
      } as any,
    ]);
    component.inventions.set([
      {
        id: 43,
        title: 'River Song',
        summary: 'A shared ritual emerges.',
        category: 'SOCIAL_PRACTICE',
        impactScore: 2,
        yearCreated: 1,
        enrichmentStatus: 'READY',
        enrichedTitle: 'Song of the River',
        enrichedSummary: 'A new tradition binds the group.',
      } as any,
    ]);

    component.selectEvent(component.events()[0] as any);
    component.selectInvention(component.inventions()[0] as any);
    await flushView();

    expect(component.chatTurns().length).toBe(2);
    expect(component.chatTurns().map((turn) => turn.kind)).toEqual([
      'story',
      'story',
    ]);
    expect(component.chatTurns()[0]?.contextSummary).toContain('Selected event');
    expect(component.chatTurns()[1]?.contextSummary).toContain(
      'Selected discovery',
    );
  });

  it('scrolls the feed to the bottom after appending a turn', async () => {
    const feed = fixture.nativeElement.querySelector('.chat-feed') as HTMLElement;
    const scrollTo = jasmine.createSpy('scrollTo');
    Object.defineProperty(feed, 'scrollHeight', { value: 640, configurable: true });
    (feed as HTMLElement & { scrollTo: typeof scrollTo }).scrollTo = scrollTo;

    component.builderActionKey.set('WORLD_STATUS');
    component.onExecuteBuilderAction();
    await flushView();

    expect(scrollTo).toHaveBeenCalledWith({ top: 640 });
  });

  it('shows start control when simulation is not running and calls start handler', () => {
    const native = fixture.nativeElement as HTMLElement;
    const startButton = native.querySelector(
      'button[aria-label="Start simulation"]',
    ) as HTMLButtonElement | null;
    const stopButton = native.querySelector(
      'button[aria-label="Stop simulation"]',
    ) as HTMLButtonElement | null;

    expect(startButton).not.toBeNull();
    expect(stopButton).toBeNull();

    startButton?.click();
    expect(cityService.startSimulation).toHaveBeenCalledWith(7);
  });

  it('shows stop control while running and keeps step disabled', () => {
    component.isRunning.set(true);
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;
    const stopButton = native.querySelector(
      'button[aria-label="Stop simulation"]',
    ) as HTMLButtonElement | null;
    const startButton = native.querySelector(
      'button[aria-label="Start simulation"]',
    ) as HTMLButtonElement | null;
    const stepButton = native.querySelector(
      'button[aria-label="Advance simulation by one step"]',
    ) as HTMLButtonElement | null;

    expect(stopButton).not.toBeNull();
    expect(startButton).toBeNull();
    expect(stepButton?.disabled).toBeTrue();

    stopButton?.click();
    expect(cityService.stopSimulation).toHaveBeenCalledWith(7);
  });
});
