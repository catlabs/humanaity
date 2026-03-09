import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  signal,
  ViewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import {
  MatSidenavModule,
  type MatDrawerMode,
} from '@angular/material/sidenav';
import { ActivatedRoute } from '@angular/router';
import { CityOutput } from '@api';
import { EventItemComponent, EventType } from '@shared';
import { of, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { CityService } from '../../city.service';
import { PixiCanvasService } from '../../services/pixi-canvas.service';

type RecentEvent = {
  id: number;
  title: string;
  description: string;
  timestamp: string;
  type: EventType;
};

@Component({
  selector: 'app-city-details',
  standalone: true,
  templateUrl: './city-details.page.html',
  styleUrl: './city-details.page.scss',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatSidenavModule,
    EventItemComponent,
  ],
})
export class CityDetailsPage implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('container', { static: true })
  containerRef!: ElementRef<HTMLDivElement>;
  private route = inject(ActivatedRoute);
  public city: CityOutput = this.route.snapshot.data['city'];
  private cityService = inject(CityService);
  private pixiCanvasService = inject(PixiCanvasService);
  private subscription?: Subscription;

  private platformId = inject(PLATFORM_ID);
  private breakpointObserver = inject(BreakpointObserver);
  private isBrowser = isPlatformBrowser(this.platformId);

  drawerOpen = signal(false);
  isMobile = toSignal(
    this.isBrowser
      ? this.breakpointObserver
          .observe('(max-width: 1023.98px)')
          .pipe(map((result) => result.matches))
      : of(false),
    { initialValue: false }
  );
  drawerMode = computed<MatDrawerMode>(() =>
    this.isMobile() ? 'over' : 'side'
  );

  // UI State
  isRunning = signal(false);
  cycle = signal(247);
  population = signal(12847);
  happiness = signal(87);
  resources = signal('$45.2K');

  private recentEventSeed: RecentEvent[] = [
    {
      id: 1,
      title: 'New citizen born',
      description: 'Sarah Johnson arrived in the city',
      timestamp: '2 minutes ago',
      type: 'birth',
    },
    {
      id: 2,
      title: 'Building completed',
      description: 'New residential complex finished',
      timestamp: '15 minutes ago',
      type: 'building',
    },
    {
      id: 3,
      title: 'New invention',
      description: 'Steam engine discovered by engineers',
      timestamp: '1 hour ago',
      type: 'invention',
    },
  ];

  recentEvents = signal<RecentEvent[]>(
    Array.from({ length: 6 }).flatMap((_, batch) =>
      this.recentEventSeed.map((event, idx) => ({
        ...event,
        id: batch * this.recentEventSeed.length + idx + 1,
      }))
    )
  );

  ngOnInit() {
    // No-op: defer Pixi init until view is ready
  }

  ngAfterViewInit(): void {
    this.pixiCanvasService
      .initialize(this.containerRef.nativeElement)
      .then(() => {
        this.subscription = this.cityService
          .subscribePositions(String(this.city.id!))
          .subscribe({
            next: (humans) => {
              humans.forEach((human) => {
                this.pixiCanvasService.addHuman(human);
                this.pixiCanvasService.updateHuman(human);
              });
            },
            error: (error) => {
              console.error('Error subscribing to positions:', error);
            },
          });
      });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.pixiCanvasService.destroy();
  }

  // Simulation Controls
  onStart(): void {
    if (this.city.id) {
      this.cityService.startSimulation(this.city.id).subscribe({
        next: () => {
          this.isRunning.set(true);
          this.checkSimulationStatus();
        },
        error: (error) => {
          console.error('Error starting simulation:', error);
        },
      });
    }
  }

  onPause(): void {
    if (this.city.id) {
      this.cityService.stopSimulation(this.city.id).subscribe({
        next: () => {
          this.isRunning.set(false);
        },
        error: (error) => {
          console.error('Error stopping simulation:', error);
        },
      });
    }
  }

  onStep(steps: number): void {
    // TODO: Implement step functionality
    console.log(`Step ${steps} cycles`);
  }

  checkSimulationStatus(): void {
    if (this.city.id) {
      this.cityService.isSimulationRunning(this.city.id).subscribe({
        next: (running) => {
          this.isRunning.set(running);
        },
        error: (error) => {
          console.error('Error checking simulation status:', error);
        },
      });
    }
  }

  openDrawer(): void {
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }
}
