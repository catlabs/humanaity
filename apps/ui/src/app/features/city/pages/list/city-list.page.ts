import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CityOverviewOutput } from '@api';
import { CityService } from '../../city.service';

type SimulationStatus = 'running' | 'paused' | 'created' | 'completed' | 'stopped';

interface SimulationRow {
  id: number;
  name: string;
  status: SimulationStatus;
  population: number;
  year: number;
  era: string;
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
  private router = inject(Router);
  private cityService = inject(CityService);

  overviews = signal<CityOverviewOutput[]>([]);
  simulations = signal<SimulationRow[]>([]);

  ngOnInit() {
    this.loadOverview();
  }

  loadOverview(): void {
    this.cityService.getSimulationOverview().subscribe({
      next: (overviews) => {
        this.overviews.set(overviews);
        this.convertCitiesToSimulations();
      },
      error: (error) => {
        console.error('Error loading simulation overview:', error);
      }
    });
  }

  convertCitiesToSimulations(): void {
    const sims = this.overviews().map((overview) => this.convertToSimulationRow(overview));
    this.simulations.set(sims);
  }

  convertToSimulationRow(overview: CityOverviewOutput): SimulationRow {
    const status = this.toSimulationStatus(overview);
    const lastUpdated = overview.updatedAt ? this.formatRelativeTime(overview.updatedAt) : 'Never';
    return {
      id: overview.cityId,
      name: overview.cityName || 'Unnamed Simulation',
      status,
      population: overview.population,
      year: overview.year,
      era: this.formatEnumLabel(overview.era),
      inventions: overview.inventionCount,
      lastUpdated
    };
  }

  private toSimulationStatus(overview: CityOverviewOutput): SimulationStatus {
    if (overview.running) {
      return 'running';
    }
    if (!overview.hasRun || !overview.runStatus) {
      return 'stopped';
    }
    switch (overview.runStatus) {
      case CityOverviewOutput.RunStatusEnum.Paused:
        return 'paused';
      case CityOverviewOutput.RunStatusEnum.Created:
        return 'created';
      case CityOverviewOutput.RunStatusEnum.Completed:
        return 'completed';
      default:
        return 'stopped';
    }
  }

  private formatEnumLabel(value: string): string {
    const lowercase = value.toLowerCase();
    return lowercase.charAt(0).toUpperCase() + lowercase.slice(1);
  }

  private formatRelativeTime(isoTimestamp: string): string {
    const timestamp = Date.parse(isoTimestamp);
    if (Number.isNaN(timestamp)) {
      return 'Unknown';
    }

    const diffMs = Date.now() - timestamp;
    const diffMinutes = Math.floor(diffMs / 60_000);
    if (diffMinutes < 1) {
      return 'Just now';
    }
    if (diffMinutes < 60) {
      return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;
    }

    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) {
      return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
    }

    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
  }

  onRowClick(simulation: SimulationRow): void {
    this.router.navigate(['/cities', simulation.id]);
  }

  onCreateNew(): void {
    this.router.navigate(['/cities/create']);
  }

  onToggleStatus(simulation: SimulationRow, event: Event): void {
    event.stopPropagation();
    if (simulation.status === 'running') {
      this.cityService.stopSimulation(simulation.id).subscribe({
        next: () => this.loadOverview(),
        error: (error) => console.error('Error stopping simulation:', error)
      });
      return;
    }
    // UI does not start simulations; use "Step" inside the simulation page to begin,
    // or start via backend tooling.
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
