import { Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { DIFICULTADES, TERRENOS, formatDuracion, formatKm, longitudTrack } from '../../../core/labels';
import { elegirCarretera, indicesMarcados, quitarUltimo } from '../../../core/tramos';
import { trazadoPorCarretera } from '../../../core/trazado';
import { PuntoTrack, leerTrack, simplificar } from '../../../core/track';
import { AlternativaTramo, Dificultad, RutaRequest, RutaResponse, SugerenciaRuta, TipoTerreno } from '../../../models/api.model';
import { toApiProblem } from '../../../services/api-error';
import { ConfirmService } from '../../../services/confirm';
import { NotifyService } from '../../../services/notify';
import { RutaService } from '../../../services/ruta';
import { StatsService } from '../../../services/stats';
import { SugerenciaService } from '../../../services/sugerencia';
import { PuntoMapa, RutaMap } from '../../shared/ruta-map/ruta-map';

const MESSAGES: Record<string, string> = {
  required: 'Obligatorio.',
  min: 'Valor demasiado bajo.',
};

@Component({
  selector: 'app-ruta-form',
  imports: [ReactiveFormsModule, RouterLink, RutaMap],
  templateUrl: './ruta-form.html',
})
export class RutaForm {
  private rutas = inject(RutaService);
  private sugerencias = inject(SugerenciaService);
  private notify = inject(NotifyService);
  private confirm = inject(ConfirmService);
  private stats = inject(StatsService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  /** Parámetro de ruta /rutas/:uuid; vacío al crear. */
  readonly uuid = input<string>();
  /** ?sugerencia=UUID: alta a partir de una sugerencia del scraper. */
  readonly sugerencia = input<string>();

  protected readonly dificultades = DIFICULTADES;
  protected readonly terrenos = TERRENOS;
  protected readonly formatKm = formatKm;
  protected readonly formatDuracion = formatDuracion;

  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    descripcion: this.fb.control<string | null>(null, Validators.maxLength(5000)),
    puntoInicio: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    puntoFin: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    duracionEstimadaMin: this.fb.control<number | null>(null, Validators.min(1)),
    dificultad: this.fb.nonNullable.control<Dificultad>('moderada'),
    tipoTerreno: this.fb.nonNullable.control<TipoTerreno>('asfalto'),
  });

  protected readonly isEdit = computed(() => this.uuid() !== undefined);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly puntos = signal<PuntoMapa[]>([]);
  /** Recorrido por carretera de los puntos marcados; la distancia pasa a ser la real. */
  protected readonly trazado = trazadoPorCarretera(this.puntos, this.rutas);
  protected readonly distancia = computed(() => this.trazado()?.distanciaKm ?? longitudTrack(this.puntos()));
  protected readonly sugerenciaCargada = signal<SugerenciaRuta | null>(null);
  /** Lugares de la sugerencia que no se pudieron geolocalizar: el admin los marca a mano. */
  protected readonly sinCoordenadas = signal<string[]>([]);
  private nombreRuta = '';

  /** Carreteras posibles para un tramo (entre dos puntos marcados seguidos) y la elegida. */
  protected readonly tramo = signal<{ desde: number; hasta: number; opciones: AlternativaTramo[]; elegida: number } | null>(null);
  /** Índice del punto de salida del tramo cuyas carreteras se están buscando. */
  protected readonly buscandoTramo = signal<number | null>(null);
  private peticionTramo = 0;

  protected readonly numMarcados = computed(() => indicesMarcados(this.puntos()).length);

  protected readonly alternativasMapa = computed(() => {
    const t = this.tramo();
    return t ? t.opciones.map((o, i) => ({ trazado: o.trazado, elegida: i === t.elegida })) : [];
  });

  /** Tramos entre puntos marcados seguidos, para elegir su carretera (no en tracks importados). */
  protected readonly tramos = computed(() => {
    const puntos = this.puntos();
    const marcados = indicesMarcados(puntos);
    if (marcados.length < 2 || puntos.length > 100) return [];
    return marcados.slice(1).map((hasta, n) => ({
      numero: n + 1,
      desde: marcados[n],
      hasta,
      nombre: `${puntos[marcados[n]].nombre ?? `Punto ${n + 1}`} → ${puntos[hasta].nombre ?? `Punto ${n + 2}`}`,
    }));
  });

  ngOnInit(): void {
    const uuid = this.uuid();
    const sugerenciaUuid = this.sugerencia();
    forkJoin({
      ruta: uuid !== undefined ? this.rutas.get(uuid) : of(null),
      sugerencia: sugerenciaUuid !== undefined ? this.sugerencias.get(sugerenciaUuid) : of(null),
    }).subscribe({
      next: ({ ruta, sugerencia }) => {
        if (ruta) this.fillFromRuta(ruta);
        if (sugerencia) this.fillFromSugerencia(sugerencia);
        this.loading.set(false);
      },
      error: (cause: unknown) => {
        this.formError.set(toApiProblem(cause).message);
        this.loading.set(false);
      },
    });
  }

  protected errorFor(field: string): string | null {
    const server = this.serverErrors()[field];
    if (server) return server;
    const control = this.form.get(field);
    if (!control || !control.touched || !control.errors) return null;
    if (control.errors['maxlength']) return `Máximo ${control.errors['maxlength'].requiredLength} caracteres.`;
    const key = Object.keys(control.errors)[0];
    return key ? (MESSAGES[key] ?? 'Valor no válido.') : null;
  }

  protected deshacer(): void {
    this.cerrarTramo();
    this.puntos.update((lista) => quitarUltimo(lista));
  }

  protected borrarRecorrido(): void {
    this.cerrarTramo();
    this.puntos.set([]);
  }

  /** Cambios desde el mapa: un clic añade un punto al final (y se buscan sus carreteras); arrastrar mueve uno. */
  protected cambiarPuntos(nuevos: PuntoMapa[]): void {
    const anteriores = this.puntos();
    this.puntos.set(nuevos);
    this.cerrarTramo();
    if (nuevos.length === anteriores.length + 1 && !nuevos[nuevos.length - 1].via) {
      const marcados = indicesMarcados(nuevos);
      if (marcados.length >= 2) this.buscarCarreteras(marcados[marcados.length - 2], marcados[marcados.length - 1], true);
    }
  }

  /** Pide las carreteras posibles entre dos puntos marcados seguidos. */
  protected buscarCarreteras(desde: number, hasta: number, automatico = false): void {
    const id = ++this.peticionTramo;
    const puntos = this.puntos();
    this.tramo.set(null);
    this.buscandoTramo.set(desde);
    this.rutas.alternativas(puntos[desde], puntos[hasta]).subscribe({
      next: (opciones) => {
        if (id !== this.peticionTramo) return;
        this.buscandoTramo.set(null);
        if (opciones.length > 1) {
          // Si ya se había elegido una carretera en este tramo, se marca la que corresponde.
          const elegida = puntos.slice(desde + 1, hasta).some((p) => p.via) ? -1 : 0;
          this.tramo.set({ desde, hasta, opciones, elegida });
        } else if (!automatico) {
          this.notify.success('En este tramo no hay otra carretera que merezca la pena.');
        }
      },
      error: (cause: unknown) => {
        if (id !== this.peticionTramo) return;
        this.buscandoTramo.set(null);
        if (!automatico) this.notify.error(toApiProblem(cause).message);
      },
    });
  }

  protected nombreTramo(desde: number): string {
    const tr = this.tramos().find((x) => x.desde === desde);
    return tr ? `${tr.numero}: ${tr.nombre}` : '';
  }

  protected elegir(indice: number): void {
    const t = this.tramo();
    if (!t) return;
    const nuevos = elegirCarretera(this.puntos(), t.desde, t.hasta, t.opciones[indice].puntosDePaso);
    this.puntos.set(nuevos);
    this.tramo.set({ ...t, hasta: t.desde + t.opciones[indice].puntosDePaso.length + 1, elegida: indice });
  }

  protected cerrarTramo(): void {
    this.peticionTramo++;
    this.tramo.set(null);
    this.buscandoTramo.set(null);
  }

  /**
   * Sustituye el recorrido por el de uno o varios ficheros GPX/KML. Varios ficheros se unen en
   * orden de nombre (una ruta publicada por tramos: «parte 1», «parte 2»...).
   */
  protected async importarTrack(input: HTMLInputElement): Promise<void> {
    const ficheros = Array.from(input.files ?? []).sort((a, b) => a.name.localeCompare(b.name, 'es', { numeric: true }));
    input.value = '';
    if (ficheros.length === 0) return;
    try {
      const todos: PuntoTrack[] = [];
      for (const fichero of ficheros) {
        todos.push(...leerTrack(await fichero.text(), fichero.name));
      }
      const simplificados = simplificar(todos);
      const { puntoInicio, puntoFin } = this.form.getRawValue();
      this.puntos.set(
        simplificados.map((p, i) => ({
          ...p,
          nombre: i === 0 ? puntoInicio || null : i === simplificados.length - 1 ? puntoFin || null : null,
        })),
      );
      this.sinCoordenadas.set([]);
      this.cerrarTramo();
      this.notify.success(
        `Recorrido importado: ${simplificados.length} puntos (de ${todos.length}) · ${formatKm(longitudTrack(simplificados))}.`,
      );
    } catch (cause: unknown) {
      this.notify.error(cause instanceof Error ? cause.message : 'No se ha podido leer el fichero.');
    }
  }

  protected save(): void {
    this.serverErrors.set({});
    this.formError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('Revisa los campos marcados.');
      return;
    }
    if (this.puntos().length < 2) {
      this.formError.set('Marca al menos dos puntos en el mapa (salida y llegada).');
      return;
    }
    this.saving.set(true);
    const request = this.buildRequest();
    const uuid = this.uuid();
    const sugerencia = this.sugerenciaCargada();

    const save$ = uuid !== undefined ? this.rutas.update(uuid, request) : this.rutas.create(request);
    save$
      .pipe(
        // Si la ruta nace de una sugerencia, se aprueba vinculándola con la ruta recién creada.
        switchMap((ruta) => (sugerencia ? this.sugerencias.approve(sugerencia.uuid, ruta.uuid).pipe(switchMap(() => of(ruta))) : of(ruta))),
      )
      .subscribe({
        next: (ruta) => {
          this.notify.success(uuid !== undefined ? `«${ruta.nombre}» guardada.` : `«${ruta.nombre}» publicada.`);
          this.stats.refresh();
          this.router.navigate([sugerencia ? '/sugerencias' : '/rutas']);
        },
        error: (cause: unknown) => {
          const problem = toApiProblem(cause);
          this.serverErrors.set(problem.fieldErrors);
          this.formError.set(problem.message);
          this.saving.set(false);
        },
      });
  }

  protected async remove(): Promise<void> {
    const uuid = this.uuid();
    if (uuid === undefined) return;
    const ok = await this.confirm.ask({
      title: 'Eliminar ruta',
      message: `Se borrará «${this.nombreRuta}» con sus valoraciones. No se puede deshacer.`,
      confirmLabel: 'Eliminar',
      danger: true,
    });
    if (!ok) return;
    this.rutas.remove(uuid).subscribe({
      next: () => {
        this.notify.success(`«${this.nombreRuta}» eliminada.`);
        this.stats.refresh();
        this.router.navigate(['/rutas']);
      },
      error: (cause: unknown) => this.notify.error(toApiProblem(cause).message),
    });
  }

  private fillFromRuta(ruta: RutaResponse): void {
    this.nombreRuta = ruta.nombre;
    this.form.patchValue({
      nombre: ruta.nombre,
      descripcion: ruta.descripcion,
      puntoInicio: ruta.puntoInicio,
      puntoFin: ruta.puntoFin,
      duracionEstimadaMin: ruta.duracionEstimadaMin,
      dificultad: ruta.dificultad,
      tipoTerreno: ruta.tipoTerreno,
    });
    const puntos = [...(ruta.puntos ?? [])].sort((a, b) => a.orden - b.orden);
    if (puntos.length > 0) {
      this.puntos.set(puntos.map((p) => ({ latitud: p.latitud, longitud: p.longitud, nombre: p.nombrePunto, via: p.via ?? false })));
    } else if (ruta.latitudInicio != null && ruta.longitudInicio != null && ruta.latitudFin != null && ruta.longitudFin != null) {
      this.puntos.set([
        { latitud: ruta.latitudInicio, longitud: ruta.longitudInicio, nombre: ruta.puntoInicio },
        { latitud: ruta.latitudFin, longitud: ruta.longitudFin, nombre: ruta.puntoFin },
      ]);
    }
  }

  private fillFromSugerencia(s: SugerenciaRuta): void {
    this.sugerenciaCargada.set(s);
    this.form.patchValue({
      nombre: s.nombre,
      descripcion: s.descripcion,
      puntoInicio: s.puntoInicio,
      puntoFin: s.puntoFin,
      duracionEstimadaMin: s.duracionEstimadaMin,
      dificultad: s.dificultad ?? 'moderada',
      tipoTerreno: s.tipoTerreno ?? 'asfalto',
    });
    const geolocalizados = s.puntos.filter((p) => p.latitud != null && p.longitud != null);
    this.puntos.set(geolocalizados.map((p) => ({ latitud: p.latitud!, longitud: p.longitud!, nombre: p.nombre })));
    this.sinCoordenadas.set(s.puntos.filter((p) => p.latitud == null || p.longitud == null).map((p) => p.nombre));
  }

  private buildRequest(): RutaRequest {
    const v = this.form.getRawValue();
    const puntos = this.puntos();
    const inicio = puntos[0];
    const fin = puntos[puntos.length - 1];
    const distancia = this.distancia();
    return {
      nombre: v.nombre.trim(),
      descripcion: v.descripcion?.trim() || null,
      puntoInicio: v.puntoInicio.trim(),
      latitudInicio: inicio.latitud,
      longitudInicio: inicio.longitud,
      puntoFin: v.puntoFin.trim(),
      latitudFin: fin.latitud,
      longitudFin: fin.longitud,
      distanciaKm: distancia > 0 ? distancia : null,
      duracionEstimadaMin: v.duracionEstimadaMin || null,
      dificultad: v.dificultad,
      tipoTerreno: v.tipoTerreno,
      puntos: puntos.map((p, orden) => ({ orden, latitud: p.latitud, longitud: p.longitud, nombrePunto: p.nombre?.slice(0, 100) ?? null, via: p.via ?? false })),
    };
  }
}
