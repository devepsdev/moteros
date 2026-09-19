import { authStore } from "@/auth/authStore";
import type { ApiResponse, ArchivoSubido } from "@/types/dto";
import { File, UploadType } from "expo-file-system";
import { ApiError } from "./client";
import { API_URL } from "./config";

/**
 * Sube una imagen elegida con expo-image-picker y devuelve la URL relativa
 * ("/uploads/xxx.jpg") que se guarda en fotoPerfilUrl / fotoUrl / imagenUrl.
 *
 * Se intenta primero con el módulo nativo de ficheros y, si falla antes de llegar al
 * servidor, con `fetch` y FormData: según el móvil y el origen de la imagen (galería nueva,
 * recorte, content://) uno u otro camino puede no funcionar.
 */
export async function subirImagen(uri: string, mimeType?: string | null, fileName?: string | null): Promise<ArchivoSubido> {
  const tipo = mimeType ?? "image/jpeg";
  try {
    return await conModuloNativo(uri, tipo);
  } catch (cause) {
    if (cause instanceof ApiError) throw cause;
    try {
      return await conFormData(uri, tipo, fileName);
    } catch (segundo) {
      if (segundo instanceof ApiError) throw segundo;
      // Ninguno de los dos ha salido del móvil: se muestra el motivo para poder arreglarlo.
      throw new ApiError(0, `No se ha podido enviar la imagen: ${detalle(cause)} / ${detalle(segundo)}`);
    }
  }
}

/** Subida nativa: el fichero no pasa por JavaScript. */
async function conModuloNativo(uri: string, tipo: string): Promise<ArchivoSubido> {
  const enviar = () =>
    new File(uri).upload(`${API_URL}/api/uploads`, {
      uploadType: UploadType.MULTIPART,
      fieldName: "file",
      mimeType: tipo,
      headers: cabeceras(),
    });

  let respuesta = await enviar();
  if ((respuesta.status === 401 || respuesta.status === 403) && (await authStore.refreshAccessToken())) {
    respuesta = await enviar();
  }
  return leerRespuesta(respuesta.status, respuesta.body);
}

/** Subida clásica con FormData. */
async function conFormData(uri: string, tipo: string, fileName?: string | null): Promise<ArchivoSubido> {
  const extension = tipo.split("/")[1] ?? "jpg";
  const enviar = async () => {
    const form = new FormData();
    // En React Native, FormData acepta {uri, name, type} para adjuntar un archivo local.
    form.append("file", { uri, name: fileName ?? `foto.${extension}`, type: tipo } as unknown as Blob);
    const res = await fetch(`${API_URL}/api/uploads`, { method: "POST", headers: cabeceras(), body: form });
    return { status: res.status, body: await res.text() };
  };

  let respuesta = await enviar();
  if ((respuesta.status === 401 || respuesta.status === 403) && (await authStore.refreshAccessToken())) {
    respuesta = await enviar();
  }
  return leerRespuesta(respuesta.status, respuesta.body);
}

function cabeceras(): Record<string, string> {
  const token = authStore.getAccessToken();
  return { Accept: "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) };
}

function leerRespuesta(status: number, body: string | undefined): ArchivoSubido {
  const cuerpo = leerJson(body);
  if (status < 200 || status >= 300) {
    throw new ApiError(status, cuerpo?.message ?? mensajePorDefecto(status));
  }
  const datos = cuerpo?.data as ArchivoSubido | undefined;
  if (!datos?.url) {
    throw new ApiError(status, "El servidor no ha devuelto la imagen subida.");
  }
  return datos;
}

function leerJson(body: string | undefined): Partial<ApiResponse<unknown>> | null {
  if (!body) return null;
  try {
    return JSON.parse(body) as Partial<ApiResponse<unknown>>;
  } catch {
    return null;
  }
}

function detalle(cause: unknown): string {
  return cause instanceof Error ? `${cause.name}: ${cause.message}` : String(cause);
}

function mensajePorDefecto(status: number): string {
  if (status === 413) return "La imagen es demasiado grande.";
  if (status >= 500) return "El servidor ha tenido un problema al guardar la imagen.";
  return "No se ha podido subir la imagen.";
}
