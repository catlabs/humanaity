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
        referencedEntities: { humanId: null, placeId: null, targetHumanId: null },
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
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('loads snapshot history and command-builder metadata on init', () => {
    expect(cityService.getSimulationSnapshot).toHaveBeenCalledWith(7);
    expect(cityService.getSimulationTimeline).toHaveBeenCalledWith(7, 100);
    expect(cityService.getSimulationCommandBuilder).toHaveBeenCalledWith(7);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Command builder');
    expect(text).toContain('Spec City');
  });

  it('executes actorless query action without requiring actor/target', () => {
    const component = fixture.componentInstance;
    component.builderActionKey.set('WORLD_STATUS');
    component.onExecuteBuilderAction();

    expect(cityService.sendSimulationAssistantCommand).toHaveBeenCalledWith(
      7,
      'world status',
    );
  });

  it('builds and submits deterministic command text for actor+target actions', () => {
    const component = fixture.componentInstance;
    component.builderActionKey.set('MOVE_HUMAN_TO_PLACE');
    component.builderActorValue.set('1');
    component.builderTargetValue.set('forest');

    component.onExecuteBuilderAction();

    expect(cityService.sendSimulationCommand).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ commandText: 'move 1 forest' }),
    );
  });

  it('prevents submission for invalid combinations', () => {
    const component = fixture.componentInstance;
    component.builderActionKey.set('MOVE_HUMAN_TO_PLACE');
    component.builderActorValue.set('');
    component.builderTargetValue.set('forest');

    expect(component.canExecuteBuilderAction()).toBeFalse();
  });

  it('removes legacy assistant chips and console feedback row', () => {
    const native = fixture.nativeElement as HTMLElement;
    expect(native.querySelector('.assistant-suggestions')).toBeNull();
    expect(native.querySelector('.console-feedback')).toBeNull();
    expect(native.querySelector('.chat-row')).toBeNull();
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
    const component = fixture.componentInstance;
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
