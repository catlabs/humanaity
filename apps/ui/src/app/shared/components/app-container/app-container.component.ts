import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-container',
  standalone: true,
  imports: [CommonModule],
  host: {
    class: 'block',
  },
  template: `
    <div class="app-container">
      <ng-content />
    </div>
  `,
})
export class AppContainerComponent {}
