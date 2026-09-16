/**
 * Tipos espejo de los DTOs del backend (dev.deveps.moteros.dto.*).
 * BigDecimal -> number; LocalDateTime -> string ISO sin zona (hora de España).
 */

export type RolUsuario = 'user' | 'admin' | 'scraper';
export type Dificultad = 'facil' | 'moderada' | 'dificil' | 'extrema';
export type TipoTerreno = 'asfalto' | 'offroad' | 'mixto';
export type EstadoSugerencia = 'pendiente' | 'aprobada' | 'rechazada';

/** Envoltorio de todas las respuestas (ApiResponseDTO). */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  /** En los errores de validación trae los mensajes por campo. */
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
  };
}

export interface UsuarioSummary {
  uuid: string;
  nombreUsuario: string;
  nombreCompleto: string;
  fotoPerfilUrl: string | null;
  ciudad: string | null;
}

export interface UsuarioResponse extends UsuarioSummary {
  email: string;
  biografia: string | null;
  fechaRegistro: string;
  activo: boolean;
  rol: RolUsuario;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
  type: string;
  usuario: UsuarioResponse;
}

export interface PuntoRuta {
  orden: number;
  latitud: number;
  longitud: number;
  nombrePunto?: string | null;
}

export interface RutaSummary {
  uuid: string;
  nombre: string;
  creador: UsuarioSummary;
  puntoInicio: string;
  puntoFin: string;
  distanciaKm: number | null;
  duracionEstimadaMin: number | null;
  dificultad: Dificultad;
  tipoTerreno: TipoTerreno;
  valoracionMedia: number | null;
  numValoraciones: number | null;
}

export interface RutaResponse extends RutaSummary {
  descripcion: string | null;
  latitudInicio: number | null;
  longitudInicio: number | null;
  latitudFin: number | null;
  longitudFin: number | null;
  fechaCreacion: string;
  puntos: PuntoRuta[] | null;
}

export interface RutaRequest {
  nombre: string;
  descripcion: string | null;
  puntoInicio: string;
  latitudInicio: number | null;
  longitudInicio: number | null;
  puntoFin: string;
  latitudFin: number | null;
  longitudFin: number | null;
  distanciaKm: number | null;
  duracionEstimadaMin: number | null;
  dificultad: Dificultad;
  tipoTerreno: TipoTerreno;
  puntos: PuntoRuta[];
}

export interface PuntoSugerido {
  nombre: string;
  latitud: number | null;
  longitud: number | null;
}

export interface EnlaceTrack {
  texto: string | null;
  url: string;
}

export interface SugerenciaRuta {
  uuid: string;
  autor: UsuarioSummary | null;
  urlFuente: string;
  nombre: string;
  descripcion: string | null;
  puntoInicio: string;
  puntoFin: string;
  distanciaKm: number | null;
  duracionEstimadaMin: number | null;
  dificultad: Dificultad | null;
  tipoTerreno: TipoTerreno | null;
  puntos: PuntoSugerido[];
  /** Enlaces de la fuente al recorrido exacto (GPX, KML) para descargarlo e importarlo. */
  enlacesTrack: EnlaceTrack[] | null;
  estado: EstadoSugerencia;
  rutaUuid: string | null;
  motivoRechazo: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface EstadisticasGlobales {
  usuariosTotales: number;
  usuariosActivos: number;
  administradores: number;
  motos: number;
  rutas: number;
  quedadas: number;
  quedadasProximas: number;
  publicaciones: number;
  comentarios: number;
  valoraciones: number;
  amistadesAceptadas: number;
  sugerenciasPendientes: number;
}
