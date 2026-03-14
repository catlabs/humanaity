import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  BoardEventMarkerViewModel,
  BoardInteractionViewModel,
  BoardMarkerViewModel,
  BoardPlaceViewModel,
} from '../../services/board-view-model.service';

@Component({
  selector: 'app-symbolic-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './symbolic-board.component.html',
  styleUrl: './symbolic-board.component.scss',
})
export class SymbolicBoardComponent {
  @Input() markers: BoardMarkerViewModel[] = [];
  @Input() places: BoardPlaceViewModel[] = [];
  @Input() interactions: BoardInteractionViewModel[] = [];
  @Input() eventMarkers: BoardEventMarkerViewModel[] = [];
  @Input() pulseNonce = 0;
  @Input() highlightedEventId: number | null = null;
  @Input() selectedHumanId: number | null = null;
  @Input() trackedHumanId: number | null = null;
  @Output() markerSelected = new EventEmitter<number>();
  @Output() eventMarkerSelected = new EventEmitter<number>();

  trackByMarkerId(_: number, marker: BoardMarkerViewModel): number {
    return marker.id;
  }

  onMarkerClick(markerId: number): void {
    this.markerSelected.emit(markerId);
  }

  onEventMarkerClick(eventId: number): void {
    this.eventMarkerSelected.emit(eventId);
  }
}
