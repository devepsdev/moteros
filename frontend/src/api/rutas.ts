import type {
  Dificultad,
  PageResponse,
  RutaRequest,
  RutaResponse,
  RutaSummary,
  TipoTerreno,
  ValoracionRequest,
  ValoracionRuta,
} from "@/types/dto";
import { request } from "./client";

export interface FiltroRutas {
  nombre?: string;
  dificultad?: Dificultad;
  tipoTerreno?: TipoTerreno;
  distanciaMaxKm?: number;
}

/** Sin filtros usa el listado general; con alguno, el endpoint de filtrado. */
export function listar(filtro: FiltroRutas, page: number, size = 20): Promise<PageResponse<RutaSummary>> {
  const conFiltros = Boolean(filtro.dificultad || filtro.tipoTerreno || filtro.distanciaMaxKm);
  if (!conFiltros) {
    return request<PageResponse<RutaSummary>>("/api/rutas", {
      params: { searchText: filtro.nombre, page, size, sortBy: "fechaCreacion", sortDir: "desc" },
    });
  }
  return request<PageResponse<RutaSummary>>("/api/rutas/filtrar", {
    params: { ...filtro, page, size, sortBy: "fechaCreacion", sortDir: "desc" },
  });
}

export function deUsuario(creadorUuid: string, page: number, size = 20): Promise<PageResponse<RutaSummary>> {
  return request<PageResponse<RutaSummary>>(`/api/rutas/usuario/${creadorUuid}`, {
    params: { page, size, sortBy: "fechaCreacion", sortDir: "desc" },
  });
}

export function obtener(uuid: string): Promise<RutaResponse> {
  return request<RutaResponse>(`/api/rutas/${uuid}`);
}

export function crear(data: RutaRequest): Promise<RutaResponse> {
  return request<RutaResponse>("/api/rutas", { method: "POST", body: data });
}

export function actualizar(uuid: string, data: RutaRequest): Promise<RutaResponse> {
  return request<RutaResponse>(`/api/rutas/${uuid}`, { method: "PUT", body: data });
}

export function eliminar(uuid: string): Promise<void> {
  return request<void>(`/api/rutas/${uuid}`, { method: "DELETE" });
}

export function valoraciones(uuid: string, page: number, size = 20): Promise<PageResponse<ValoracionRuta>> {
  return request<PageResponse<ValoracionRuta>>(`/api/rutas/${uuid}/valoraciones`, {
    params: { page, size, sortBy: "fecha", sortDir: "desc" },
  });
}

export function valorar(uuid: string, data: ValoracionRequest): Promise<ValoracionRuta> {
  return request<ValoracionRuta>(`/api/rutas/${uuid}/valoraciones`, { method: "PUT", body: data });
}

export function eliminarMiValoracion(uuid: string): Promise<void> {
  return request<void>(`/api/rutas/${uuid}/valoraciones`, { method: "DELETE" });
}
