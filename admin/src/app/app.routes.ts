import { Routes } from '@angular/router';
import { Login } from './components/auth/login/login';
import { Dashboard } from './components/dashboard/dashboard';
import { RutaForm } from './components/rutas/ruta-form/ruta-form';
import { RutaList } from './components/rutas/ruta-list/ruta-list';
import { SugerenciaList } from './components/sugerencias/sugerencia-list/sugerencia-list';
import { UsuarioList } from './components/usuarios/usuario-list/usuario-list';
import { adminGuard } from './guards/admin-guard';
import { AdminLayout } from './layouts/admin-layout/admin-layout';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Acceso · moter@s' },
  {
    path: '',
    component: AdminLayout,
    canActivate: [adminGuard],
    children: [
      { path: '', component: Dashboard, title: 'Resumen · moter@s' },
      { path: 'rutas', component: RutaList, title: 'Rutas · moter@s' },
      { path: 'rutas/nueva', component: RutaForm, title: 'Nueva ruta · moter@s' },
      { path: 'rutas/:uuid', component: RutaForm, title: 'Editar ruta · moter@s' },
      { path: 'sugerencias', component: SugerenciaList, title: 'Sugerencias · moter@s' },
      { path: 'usuarios', component: UsuarioList, title: 'Usuarios · moter@s' },
    ],
  },
  { path: '**', redirectTo: '' },
];
