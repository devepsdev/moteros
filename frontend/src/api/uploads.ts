import type { ArchivoSubido } from "@/types/dto";
import { request } from "./client";

/**
 * Sube una imagen elegida con expo-image-picker y devuelve la URL relativa
 * ("/uploads/xxx.jpg") que se guarda en fotoPerfilUrl / fotoUrl / imagenUrl.
 */
export function subirImagen(uri: string, mimeType?: string | null, fileName?: string | null): Promise<ArchivoSubido> {
  const tipo = mimeType ?? "image/jpeg";
  const extension = tipo.split("/")[1] ?? "jpg";
  const form = new FormData();
  // En React Native, FormData acepta {uri, name, type} para adjuntar un archivo local.
  form.append("file", { uri, name: fileName ?? `foto.${extension}`, type: tipo } as unknown as Blob);
  return request<ArchivoSubido>("/api/uploads", { method: "POST", body: form });
}
