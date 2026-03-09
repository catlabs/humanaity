import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type EventType = 'birth' | 'building' | 'invention' | 'warning' | 'marriage' | 'other';

@Component({
  selector: 'app-event-item',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule
  ],
  templateUrl: './event-item.component.html',
  styleUrl: './event-item.component.scss'
})
export class EventItemComponent {
  title = input.required<string>();
  description = input.required<string>();
  timestamp = input.required<string>();
  type = input<EventType>('other');

  getIcon(): string {
    const iconMap: Record<EventType, string> = {
      'birth': 'add_circle',
      'building': 'business',
      'invention': 'lightbulb',
      'warning': 'warning',
      'marriage': 'favorite',
      'other': 'info'
    };
    return iconMap[this.type()] || 'info';
  }

  getIconColor(): string {
    const colorMap: Record<EventType, string> = {
      'birth': '#10b981',
      'building': '#3b82f6',
      'invention': '#f59e0b',
      'warning': '#ef4444',
      'marriage': '#a855f7',
      'other': '#6b7280'
    };
    return colorMap[this.type()] || '#6b7280';
  }

  getBackgroundColor(): string {
    const bgMap: Record<EventType, string> = {
      'birth': '#d1fae5',
      'building': '#dbeafe',
      'invention': '#fef3c7',
      'warning': '#fee2e2',
      'marriage': '#f3e8ff',
      'other': '#f3f4f6'
    };
    return bgMap[this.type()] || '#f3f4f6';
  }
}


