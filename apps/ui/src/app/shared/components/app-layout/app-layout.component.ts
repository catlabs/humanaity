import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, ActivatedRoute } from '@angular/router';
import { AppHeaderComponent } from '../app-header/app-header.component';


interface HeaderRouteData {
  showUserMenu?: boolean;
  userAvatar?: string;
  userName?: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    AppHeaderComponent
  ],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss'
})
export class AppLayoutComponent {
  private route = inject(ActivatedRoute);

  // Get route data, merging from current route and all parent routes
  private routeData = computed(() => {
    let data: HeaderRouteData = {};
    let currentRoute = this.route.snapshot;
    
    // Traverse up the route tree to collect all data
    while (currentRoute) {
      data = { ...data, ...currentRoute.data };
      currentRoute = currentRoute.parent!;
    }
    
    return data;
  });

  // Header props with defaults
  showUserMenu = computed(() => this.routeData().showUserMenu ?? true);
  userAvatar = computed(() => this.routeData().userAvatar);
  userName = computed(() => this.routeData().userName);
}
