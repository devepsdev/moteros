/**
 * Tipos de la API de moter@s (backend Spring Boot). Espejo de los DTOs de
 * `backend/src/main/java/dev/deveps/moteros/dto`. Las fechas llegan como ISO sin zona
 * ("2026-09-13T18:10:42") y los BigDecimal como number.
 */

// ===================== ENVOLTORIOS =====================

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    page: number;
    size: number;
    sort: string;
    totalPages: number;
    totalElements: number;
    numberOfElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
  };
}

// ===================== ENUMS =====================

export type RolUsuario = "user" | "admin";
export type TipoMoto = "naked" | "trail" | "custom" | "deportiva" | "touring" | "scooter" | "clasica";
export type Dificultad = "facil" | "moderada" | "dificil" | "extrema";
export type TipoTerreno = "asfalto" | "offroad" | "mixto";

// ===================== AUTH =====================

export interface LoginRequest {
  identificador: string;
  password: string;
}

export interface RegistroRequest {
  nombreUsuario: string;
  nombreCompleto: string;
  email: string;
  password: string;
  ciudad?: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  expiresIn: number;
  refreshToken: string;
  usuario: UsuarioResponse;
}

export interface CambioPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}

export interface RestablecerPasswordRequest {
  email: string;
  codigo: string;
  passwordNueva: string;
}

// ===================== USUARIOS =====================

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
  numMotos: number | null;
  numRutas: number | null;
  numAmigos: number | null;
}

export interface UsuarioRequest {
  nombreUsuario: string;
  nombreCompleto: string;
  ciudad?: string | null;
  biografia?: string | null;
  fotoPerfilUrl?: string | null;
}

// ===================== MOTOS =====================

export interface MotoResponse {
  uuid: string;
  propietario: UsuarioSummary;
  marca: string;
  modelo: string;
  anio: number | null;
  cilindradaCc: number | null;
  tipo: TipoMoto;
  fotoUrl: string | null;
}

export interface MotoRequest {
  marca: string;
  modelo: string;
  anio?: number | null;
  cilindradaCc?: number | null;
  tipo?: TipoMoto;
  fotoUrl?: string | null;
}

// ===================== RUTAS =====================

export interface PuntoRuta {
  uuid?: string;
  orden: number;
  latitud: number;
  longitud: number;
  altitudM?: number | null;
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
  descripcion?: string | null;
  puntoInicio: string;
  latitudInicio?: number | null;
  longitudInicio?: number | null;
  puntoFin: string;
  latitudFin?: number | null;
  longitudFin?: number | null;
  distanciaKm?: number | null;
  duracionEstimadaMin?: number | null;
  dificultad?: Dificultad;
  tipoTerreno?: TipoTerreno;
  puntos?: PuntoRuta[];
}

export interface ValoracionRuta {
  uuid: string;
  rutaUuid: string;
  autor: UsuarioSummary;
  puntuacion: number;
  comentario: string | null;
  fecha: string;
}

export interface ValoracionRequest {
  puntuacion: number;
  comentario?: string | null;
}

// ===================== PUBLICACIONES =====================

export interface Comentario {
  uuid: string;
  publicacionUuid: string;
  autor: UsuarioSummary;
  contenido: string;
  fecha: string;
}

export interface Publicacion {
  uuid: string;
  autor: UsuarioSummary;
  ruta: RutaSummary | null;
  contenido: string;
  imagenUrl: string | null;
  fechaPublicacion: string;
  numLikes: number | null;
  numComentarios: number | null;
  likeUsuarioActual: boolean | null;
  comentarios: Comentario[] | null;
}

export interface PublicacionRequest {
  contenido: string;
  imagenUrl?: string | null;
  rutaUuid?: string | null;
}

// ===================== UPLOADS =====================

export interface ArchivoSubido {
  url: string;
  nombreArchivo: string;
  tipoContenido: string;
  tamano: number;
}
