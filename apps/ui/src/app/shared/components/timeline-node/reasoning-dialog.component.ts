import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ReasoningDialogData {
  title: string;
  period: string;
  reasoning: string;
}

@Component({
  selector: 'app-reasoning-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="period">{{ data.period }}</p>
      <div class="reasoning-section">
        <h3 class="reasoning-label">AI Reasoning</h3>
        <p class="reasoning-text">{{ data.reasoning }}</p>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="true">
        <mat-icon>close</mat-icon>
        Close
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      padding: 1.5rem;
      min-width: 400px;
      max-width: 600px;
    }

    .period {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
      margin: 0 0 1.5rem 0;
    }

    .reasoning-section {
      margin-top: 1rem;
    }

    .reasoning-label {
      font-weight: 500;
      color: var(--mat-sys-on-surface);
      margin: 0 0 0.75rem 0;
      font: var(--mat-sys-body-small);
    }

    .reasoning-text {
      color: var(--mat-sys-on-surface-variant);
      line-height: 1.6;
      margin: 0;
      font: var(--mat-sys-body-small);
    }

    mat-dialog-actions {
      padding: 1rem 1.5rem;
    }

    button mat-icon {
      margin-right: 0.5rem;
    }
  `]
})
export class ReasoningDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReasoningDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReasoningDialogData
  ) {}
}

