import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

export type SimulationStatus = 'running' | 'completed' | 'draft' | 'queued';

export interface SimulationCardData {
  id: number;
  name: string;
  description: string;
  status: SimulationStatus;
  icon: string;
  iconColor: string;
  lastRun?: string;
  runtime?: string;
  progress?: number;
  startedAt?: string;
  createdAt?: string;
  scheduledFor?: string;
  estimatedRuntime?: string;
}

@Component({
  selector: 'app-simulation-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './simulation-card.component.html',
  styleUrl: './simulation-card.component.scss'
})
export class SimulationCardComponent {
  simulation = input.required<SimulationCardData>();
  cardClick = output<number>();

  getStatusClass(status: SimulationStatus): string {
    const statusMap: Record<SimulationStatus, string> = {
      'running': 'status-running',
      'completed': 'status-completed',
      'draft': 'status-draft',
      'queued': 'status-queued'
    };
    return statusMap[status] || 'status-draft';
  }

  getStatusLabel(status: SimulationStatus): string {
    const labelMap: Record<SimulationStatus, string> = {
      'running': 'Running',
      'completed': 'Completed',
      'draft': 'Draft',
      'queued': 'Queued'
    };
    return labelMap[status] || 'Draft';
  }

  onCardClick(): void {
    this.cardClick.emit(this.simulation().id);
  }
}


