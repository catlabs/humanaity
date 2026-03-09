import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CityOutput } from '@api';
import { CityService } from '../../city.service';

type SimulationStatus = 'running' | 'paused' | 'stopped';
type Era = 'ancient' | 'medieval' | 'industrial' | 'modern';

interface SimulationRow {
  id: number;
  name: string;
  status: SimulationStatus;
  population: number;
  year: number;
  era: Era;
  inventions: number;
  lastUpdated: string;
}

@Component({
  selector: 'app-city-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './city-list.page.html',
  styleUrl: './city-list.page.scss'
})
export class CityListPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cityService = inject(CityService);

  cities = signal<CityOutput[]>([]);
  simulations = signal<SimulationRow[]>([]);

  ngOnInit() {
    // Load cities from route data or service
    const routeData = this.route.snapshot.data['cities'];
    if (routeData) {
      this.cities.set(routeData);
      this.convertCitiesToSimulations();
    } else {
      this.loadCities();
    }
  }

  loadCities(): void {
    this.cityService.getCities().subscribe({
      next: (cities) => {
        this.cities.set(cities);
        this.convertCitiesToSimulations();
      },
      error: (error) => {
        console.error('Error loading cities:', error);
      }
    });
  }

  convertCitiesToSimulations(): void {
    const sims = this.cities().map(city => this.convertToSimulationRow(city));
    this.simulations.set(sims);
  }

  convertToSimulationRow(city: CityOutput): SimulationRow {
    // Derive population from humans array length
    const population = city.humans?.length || 0;
    
    // Mock/derive other fields - TODO: Replace with actual data from simulation service
    const statuses: SimulationStatus[] = ['running', 'paused', 'stopped'];
    const status = statuses[Math.floor(Math.random() * statuses.length)] as SimulationStatus;
    
    // Derive year and era from population or use defaults
    const year = 500 + (population * 10);
    const eras: Era[] = ['ancient', 'medieval', 'industrial', 'modern'];
    const era = eras[Math.floor(year / 500) % eras.length] as Era;
    
    // Mock inventions count (could be derived from actual data later)
    const inventions = Math.floor(population * 0.5);
    
    // Mock last updated (could use actual timestamp from city data)
    const lastUpdated = this.getRelativeTime(city.id || 0);
    
    return {
      id: city.id!,
      name: city.name || 'Unnamed Simulation',
      status,
      population,
      year,
      era,
      inventions,
      lastUpdated
    };
  }

  getRelativeTime(cityId: number): string {
    // Mock relative time based on city ID
    const times = ['2 minutes ago', '1 hour ago', '5 minutes ago', '3 days ago'];
    return times[cityId % times.length];
  }

  onRowClick(simulation: SimulationRow): void {
    this.router.navigate(['/cities', simulation.id]);
  }

  onCreateNew(): void {
    this.router.navigate(['/cities/create']);
  }

  onToggleStatus(simulation: SimulationRow, event: Event): void {
    event.stopPropagation();
    // TODO: Implement status toggle logic
    const newStatus: SimulationStatus = simulation.status === 'running' ? 'paused' : 'running';
    this.simulations.update(sims => 
      sims.map(s => s.id === simulation.id ? { ...s, status: newStatus } : s)
    );
  }

  onDelete(simulation: SimulationRow, event: Event): void {
    event.stopPropagation();
    // TODO: Implement delete logic
    if (confirm(`Are you sure you want to delete "${simulation.name}"?`)) {
      this.cityService.deleteCity(simulation.id.toString()).subscribe({
        next: () => {
          this.simulations.update(sims => sims.filter(s => s.id !== simulation.id));
        },
        error: (error) => {
          console.error('Error deleting city:', error);
        }
      });
    }
  }

  getStatusColor(status: SimulationStatus): string {
    switch (status) {
      case 'running':
        return '#34D399'; // green
      case 'paused':
        return '#F59E0B'; // yellow/amber
      case 'stopped':
        return '#6B7280'; // gray
      default:
        return '#6B7280';
    }
  }
}
