import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { CityService } from '../../city.service';
import { PixiCanvasService } from '../../services/pixi-canvas.service';
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
    ]);

    cityService.getSimulationSnapshot.and.returnValue(of(snapshot));
    cityService.getSimulationTimeline.and.returnValue(of(timeline));
    cityService.startSimulation.and.returnValue(of(void 0));
    cityService.stopSimulation.and.returnValue(of(void 0));
    cityService.stepSimulation.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [SimulationDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { city: { id: 7, name: 'Spec City' } } } },
        },
        { provide: CityService, useValue: cityService },
        {
          provide: PixiCanvasService,
          useValue: {
            initialize: () => Promise.resolve(),
            addHuman: () => undefined,
            updateHuman: () => undefined,
            destroy: () => undefined,
          },
        },
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
    expect(text).toContain('No simulation run yet');
  });

  it('steps simulation through backend service', () => {
    fixture.componentInstance.onStep();
    expect(cityService.stepSimulation).toHaveBeenCalledWith(7);
  });

  it('renders backend enrichment fields for selected invention and event', () => {
    const component = fixture.componentInstance;
    component.selectInvention(component.inventions()[0]);
    fixture.detectChanges();

    let text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Canal Layout (Field Note)');
    expect(text).toContain('Fallback: Basic canal planning pattern.');
    expect(text).toContain('Enrichment: Fallback');

    component.selectEvent(component.events()[0]);
    fixture.detectChanges();

    text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Two neighbors agreed on a shared water route.');
    expect(text).toContain('Enrichment: Ready');
  });
});
