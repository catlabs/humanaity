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
      'sendAgentChat',
    ]);

    cityService.getSimulationSnapshot.and.returnValue(of(snapshot));
    cityService.getSimulationTimeline.and.returnValue(of(timeline));
    cityService.startSimulation.and.returnValue(of(void 0));
    cityService.stopSimulation.and.returnValue(of(void 0));
    cityService.stepSimulation.and.returnValue(of(void 0));
    cityService.sendAgentChat.and.returnValue(
      of({
        conversationId: 'conv-1',
        message: 'Advanced the city by 3 step(s). Current tick is 3.',
        commandClass: 'SAFE_MVP',
        executedActions: [],
        referencedEntities: { cityId: 7 },
        uiEffects: [],
      } as any)
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
    fixture.componentInstance.statusPanelVisible.set(true);
    fixture.detectChanges();
  });

  it('loads snapshot and history on init', () => {
    expect(cityService.getSimulationSnapshot).toHaveBeenCalledWith(7);
    expect(cityService.getSimulationTimeline).toHaveBeenCalledWith(7, 100);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Spec City');
    expect(text).toContain('No simulation run yet');
    expect(fixture.nativeElement.querySelector('app-simulation-board')).not.toBeNull();
  });

  it('steps simulation through backend service', () => {
    fixture.componentInstance.onStep();
    expect(cityService.stepSimulation).toHaveBeenCalledWith(7);
  });

  it('submits chat requests and renders the backend reply', () => {
    const component = fixture.componentInstance;
    component.onChatInput('advance by 3 steps');
    component.onSendChat();
    fixture.detectChanges();

    expect(cityService.sendAgentChat).toHaveBeenCalledWith(
      7,
      jasmine.objectContaining({ message: 'advance by 3 steps' })
    );

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Chat Control');
    expect(text).toContain('Advanced the city by 3 step(s). Current tick is 3.');
    expect(text).toContain('Parsed via DETERMINISTIC_MATCH');
  });

  it('applies uiEffects refresh and highlight signals from backend chat responses', () => {
    cityService.sendAgentChat.and.returnValue(
      of({
        conversationId: 'conv-2',
        message: 'Refreshed and highlighted latest event.',
        commandClass: 'SAFE_MVP',
        interpretationProvenance: 'DETERMINISTIC_MATCH',
        interpretedCommandSummary: 'EXPLAIN_EVENT',
        executedActions: [],
        referencedEntities: { cityId: 7, eventIds: [12] },
        uiEffects: [
          { type: 'REFRESH_SNAPSHOT' },
          { type: 'REFRESH_TIMELINE' },
          { type: 'HIGHLIGHT_EVENT', eventId: 12 },
        ],
      } as any)
    );

    const beforeSnapshotCalls = cityService.getSimulationSnapshot.calls.count();
    const beforeTimelineCalls = cityService.getSimulationTimeline.calls.count();

    fixture.componentInstance.onChatInput('explain event 12');
    fixture.componentInstance.onSendChat();
    fixture.detectChanges();

    expect(cityService.getSimulationSnapshot.calls.count()).toBeGreaterThan(
      beforeSnapshotCalls
    );
    expect(cityService.getSimulationTimeline.calls.count()).toBeGreaterThan(
      beforeTimelineCalls
    );
    expect(fixture.componentInstance.selectedEventId()).toBe(12);
  });

  it('renders guided compare and follow context from structured backend data', () => {
    cityService.sendAgentChat.and.returnValue(
      of({
        conversationId: 'conv-guided',
        message: 'Compared and followed selected humans.',
        commandClass: 'GUIDED',
        executedActions: [],
        referencedEntities: { cityId: 7, humanIds: [101, 202] },
        uiEffects: [{ type: 'FOCUS_HUMAN', humanId: 101 }],
        structuredData: {
          compareHumans: {
            left: { id: 101, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
            right: { id: 202, name: 'Ben', busy: true, x: 0.3, y: 0.4 },
          },
          followHuman: {
            human: { id: 101, name: 'Ada', busy: false, x: 0.1, y: 0.2 },
            ticks: 5,
            fromTick: 20,
            resultTick: 24,
            eventWindow: [],
            inventionWindow: [],
          },
        },
      } as any)
    );

    fixture.componentInstance.onChatInput('follow ada for 5 ticks');
    fixture.componentInstance.onSendChat();
    fixture.detectChanges();

    expect(fixture.componentInstance.guidedComparison()).not.toBeNull();
    expect(fixture.componentInstance.guidedFollow()?.ticks).toBe(5);
    expect(fixture.componentInstance.trackedHumanId()).toBe(101);

    expect(fixture.componentInstance.guidedComparison()).not.toBeNull();
    expect(fixture.componentInstance.guidedFollow()).not.toBeNull();
  });

  it('requires explicit confirmation for director interventions and sends token on confirm', () => {
    cityService.sendAgentChat.and.returnValues(
      of({
        conversationId: 'conv-director-1',
        message: 'Director command requested.',
        commandClass: 'DIRECTOR',
        executedActions: [{ type: 'INTERVENTION_CONFIRMATION_REQUIRED', status: 'PENDING', summary: '' }],
        referencedEntities: { cityId: 7, humanIds: [12, 34] },
        uiEffects: [],
        structuredData: {
          directorConfirmation: {
            interventionId: 77,
            commandType: 'DIRECTOR_MEET_HUMANS',
            confirmationToken: 'token-abc',
            expiresAt: '2026-03-14T14:30:00Z',
            humanIds: [12, 34],
          },
        },
      } as any),
      of({
        conversationId: 'conv-director-1',
        message: 'Intervention executed.',
        commandClass: 'DIRECTOR',
        executedActions: [{ type: 'INTERVENTION_EXECUTED', status: 'COMPLETED', summary: '' }],
        referencedEntities: { cityId: 7, humanIds: [12, 34] },
        uiEffects: [{ type: 'REFRESH_SNAPSHOT' }],
        structuredData: {
          directorIntervention: {
            id: 77,
            status: 'EXECUTED',
            commandType: 'DIRECTOR_MEET_HUMANS',
            humanIds: [12, 34],
            executedTick: 9,
          },
        },
      } as any)
    );

    const component = fixture.componentInstance;
    component.onChatInput('director make 12 and 34 meet');
    component.onSendChat();
    fixture.detectChanges();

    expect(component.pendingDirectorConfirmation()).not.toBeNull();
    expect(component.directorBoardState()).toBe('pending');

    component.onConfirmDirectorIntervention();
    fixture.detectChanges();

    expect(cityService.sendAgentChat.calls.count()).toBe(2);
    const secondCall = cityService.sendAgentChat.calls.argsFor(1)[1] as any;
    expect(secondCall.confirmIntervention).toBeTrue();
    expect(secondCall.confirmationToken).toBe('token-abc');
  });

  it('renders backend enrichment fields for selected invention and event', () => {
    const component = fixture.componentInstance;
    const invention = component.inventions()[0];
    const event = component.events()[0];

    expect(component.inventionDisplayTitle(invention)).toBe('Canal Layout (Field Note)');
    expect(component.inventionDisplaySummary(invention)).toBe('Fallback: Basic canal planning pattern.');
    expect(component.inventionEnrichmentStatusLabel(invention)).toBe('Fallback');

    expect(component.eventNarrative(event)).toBe('Two neighbors agreed on a shared water route.');
    expect(component.eventEnrichmentStatusLabel(event)).toBe('Ready');
  });
});
