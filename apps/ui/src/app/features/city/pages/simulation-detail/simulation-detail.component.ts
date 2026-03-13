import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CityOutput } from '@api';
import { CityService } from '../../city.service';

type HumanState =
  | 'active'
  | 'resting'
  | 'creating'
  | 'collaborating'
  | 'contemplating';

type InventionCategory = 'scientific' | 'philosophical' | 'cultural';

type Human = {
  id: string;
  name: string;
  state: HumanState;
  primaryTrait: string;
};

type Invention = {
  id: string;
  name: string;
  category: InventionCategory;
  year: number;
  impact: number; // 0-100
};

@Component({
  selector: 'app-simulation-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatDividerModule,
    MatSidenavModule,
    MatListModule,
  ],
  templateUrl: './simulation-detail.component.html',
  styleUrl: './simulation-detail.component.scss',
})
export class SimulationDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private cityService = inject(CityService);

  city: CityOutput = this.route.snapshot.data['city'];

  // View model (placeholder data; structure-focused)
  readonly humanStates: HumanState[] = [
    'active',
    'creating',
    'collaborating',
    'contemplating',
    'resting',
  ];

  filters = signal<Record<HumanState, boolean>>({
    active: true,
    resting: true,
    creating: true,
    collaborating: true,
    contemplating: true,
  });

  humans = signal<Human[]>([]);

  inventions = signal<Invention[]>([]);

  selectedHuman = signal<Human | null>(null);
  selectedInvention = signal<Invention | null>(null);

  filteredHumans = computed(() =>
    this.humans().filter((h) => this.filters()[h.state])
  );

  currentEra = signal('Founding');
  currentYear = signal(1);
  worldPhase = signal('idle');

  populationTotal = computed(() => this.humans().length);
  populationActive = computed(
    () => this.humans().filter((h) => h.state === 'active').length
  );

  inventionCounts = computed(() => {
    const inv = this.inventions();
    return {
      scientific: inv.filter((i) => i.category === 'scientific').length,
      philosophical: inv.filter((i) => i.category === 'philosophical').length,
      cultural: inv.filter((i) => i.category === 'cultural').length,
      total: inv.length,
    };
  });

  toggleFilter(state: HumanState): void {
    this.filters.update((prev) => ({ ...prev, [state]: !prev[state] }));
  }

  ngOnInit(): void {
    const cityId = this.city.id;
    if (!cityId) {
      return;
    }
    this.cityService.getSimulationSnapshot(cityId).subscribe({
      next: (snapshot) => {
        this.currentEra.set(this.formatEnumLabel(snapshot.run.era));
        this.currentYear.set(snapshot.run.year);
        this.worldPhase.set(snapshot.run.running ? 'running' : (snapshot.run.status?.toLowerCase() ?? 'idle'));

        this.humans.set(snapshot.humans.map((human) => ({
          id: String(human.id),
          name: human.name || `Human ${human.id}`,
          state: human.busy ? 'resting' : 'active',
          primaryTrait: human.busy ? 'Focused' : 'Curious',
        })));

        this.inventions.set(snapshot.recentInventions.map((invention) => ({
          id: invention.inventionKey,
          name: invention.title,
          category: this.toInventionCategory(invention.category),
          year: invention.yearCreated,
          impact: invention.impactScore,
        })));
      },
      error: (error) => {
        console.error('Error loading simulation snapshot:', error);
      },
    });
  }

  private toInventionCategory(category: string): InventionCategory {
    switch (category) {
      case 'TECHNIQUE':
        return 'scientific';
      case 'SOCIAL_PRACTICE':
        return 'cultural';
      default:
        return 'philosophical';
    }
  }

  private formatEnumLabel(value: string): string {
    const lower = value.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }

  selectHuman(human: Human): void {
    this.selectedHuman.set(human);
    this.selectedInvention.set(null);
  }

  clearSelection(): void {
    this.selectedHuman.set(null);
    this.selectedInvention.set(null);
  }

  selectInvention(invention: Invention): void {
    this.selectedInvention.set(invention);
    this.selectedHuman.set(null);
  }
}
