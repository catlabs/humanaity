import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';

export interface EntityData {
  name: string;
  type: string;
  icon: string;
  iconColor: string;
  status: string;
  level?: number;
  capacity?: { current: number; max: number };
  efficiency?: number;
  workers?: number;
  maintenanceCost?: string;
  builtAt?: string;
}

@Component({
  selector: 'app-entity-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatChipsModule
  ],
  templateUrl: './entity-panel.component.html',
  styleUrl: './entity-panel.component.scss'
})
export class EntityPanelComponent {
  entity = input.required<EntityData>();
  viewDetails = output<void>();

  getCapacityPercentage(): number {
    const entityData = this.entity();
    if (!entityData.capacity) return 0;
    return (entityData.capacity.current / entityData.capacity.max) * 100;
  }
}


