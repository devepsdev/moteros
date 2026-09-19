import { authStore } from "@/auth/authStore";
import type { ApiResponse, ArchivoSubido } from "@/types/dto";
import { File, UploadType } from "expo-file-system";
import { ApiError } from "./client";
import { API_URL } from "./config";

/**
 * Sube una imagen elegida con expo-image-picker y devuelve la URL relativa
 * ("/uploads/xxx.jpg") que se guarda en fotoPerfilUrl / fotoUrl / imagenUrl.
 *
 * La subida la hace el módulo nativo de ficheros (no `fetch` con FormData): en Android, el
 * envío del fichero desde JavaScript falla antes de salir del móvil.
 */
export async function subirImagen(uri: string, mimeType?: string | null): Promise<ArchivoSubido> {
  let respuesta = await enviar(uri, mimeType);
  if (respuesta.status === 401 || respuesta.status === 403) {
    if (await authStore.refreshAccessToken()) {
      respuesta = await enviar(uri, mimeType);
    }
  }

  const cuerpo = leerJson(respuesta.body);
  if (respuesta.status < 200 || respuesta.status >= 300) {
    throw new ApiError(respuesta.status, cuerpo?.message ?? mensajePorDefecto(respuesta.status));
  }
  const datos = cuerpo?.data as ArchivoSubido | undefined;
  if (!datos?.url) {
    throw new ApiError(respuesta.status, "El servidor no ha devuelto la imagen subida.");
  }
  return datos;
}

function enviar(uri: string, mimeType?: string | null) {
  const token = authStore.getAccessToken();
  // El backend decide la extensión por el tipo de contenido, no por el nombre del fichero.
  const tipo = mimeType ?? "image/jpeg";
  return new File(uri).upload(`${API_URL}/api/uploads`, {
    uploadType: UploadType.MULTIPART,
    fieldName: "file",
    mimeType: tipo,
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}

function leerJson(body: string | undefined): Partial<ApiResponse<unknown>> | null {
  if (!body) return null;
  try {
    return JSON.parse(body) as Partial<ApiResponse<unknown>>;
  } catch {
    return null;
  }
}

function mensajePorDefecto(status: number): string {
  if (status === 413) return "La imagen es demasiado grande.";
  if (status >= 500) return "El servidor ha tenido un problema al guardar la imagen.";
  return "No se ha podido subir la imagen.";
}
