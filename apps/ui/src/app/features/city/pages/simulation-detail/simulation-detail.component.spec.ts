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
});
