import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CityOutput } from '@api';

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
    MatMenuModule,
    MatDividerModule,
    MatSidenavModule,
    MatListModule,
  ],
  templateUrl: './simulation-detail.component.html',
  styleUrl: './simulation-detail.component.scss',
})
export class SimulationDetailComponent {
  private route = inject(ActivatedRoute);

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

  humans = signal<Human[]>([
    { id: 'h-001', name: 'Human 1', state: 'active', primaryTrait: 'Rational' },
    { id: 'h-002', name: 'Human 2', state: 'creating', primaryTrait: 'Creative' },
    {
      id: 'h-003',
      name: 'Human 3',
      state: 'collaborating',
      primaryTrait: 'Cooperative',
    },
    {
      id: 'h-004',
      name: 'Human 4',
      state: 'contemplating',
      primaryTrait: 'Spiritual',
    },
    { id: 'h-005', name: 'Human 5', state: 'resting', primaryTrait: 'Skeptical' },
  ]);

  inventions = signal<Invention[]>([
    {
      id: 'inv-001',
      name: 'Fire Mastery',
      category: 'scientific',
      year: 12,
      impact: 95,
    },
    {
      id: 'inv-002',
      name: 'Oral Storytelling',
      category: 'cultural',
      year: 31,
      impact: 92,
    },
    {
      id: 'inv-003',
      name: 'The Question of Existence',
      category: 'philosophical',
      year: 24,
      impact: 88,
    },
  ]);

  selectedHuman = signal<Human | null>(null);
  selectedInvention = signal<Invention | null>(null);

  filteredHumans = computed(() =>
    this.humans().filter((h) => this.filters()[h.state])
  );

  // Basic “info bar” values (placeholder)
  currentEra = signal('renaissance');
  currentYear = signal(247);
  worldPhase = signal('enlightenment');

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

  generateInvention(category: InventionCategory): void {
    const next: Invention = {
      id: `inv-${Date.now()}`,
      name: `New ${category} discovery`,
      category,
      year: this.currentYear(),
      impact: Math.floor(Math.random() * 30) + 60,
    };
    this.inventions.update((prev) => [next, ...prev]);
    this.selectedInvention.set(next);
    this.selectedHuman.set(null);
  }
}

