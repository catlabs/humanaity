import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'login',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'signup',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'cities',
    renderMode: RenderMode.Server
  },
  {
    path: 'cities/:id',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin',
    renderMode: RenderMode.Server
  },
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
