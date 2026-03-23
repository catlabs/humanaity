import {Routes} from '@angular/router';
import {cityDetailsResolver, cityListResolver} from './city.resolver';

export const cityRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/list/city-list.page').then(m => m.CityListPage),
    resolve: {cities: cityListResolver}
  },
  {
    path: 'create',
    loadComponent: () => import('./pages/create/create-city.page').then(m => m.CreateCityPage)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/simulation-detail/simulation-detail.component').then(
        (m) => m.SimulationDetailComponent
      ),
    resolve: {city: cityDetailsResolver}
  }
];
