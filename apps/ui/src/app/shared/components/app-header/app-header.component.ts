import { Component, input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@features/auth';
import { ThemeService } from '@core';


@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss'
})
export class AppHeaderComponent {
  private authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);
  readonly theme = inject(ThemeService);

  showUserMenu = input(true);
  userAvatar = input<string | undefined>();
  userName = input<string | undefined>();

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  toggleTheme(): void {
    this.theme.toggle();
  }
}


