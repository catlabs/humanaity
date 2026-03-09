import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { AppContainerComponent } from '../app-container/app-container.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatIconModule,
    AppContainerComponent
  ],
  templateUrl: './app-footer.component.html',
  styleUrl: './app-footer.component.scss'
})
export class AppFooterComponent {
  currentYear = new Date().getFullYear();
}


