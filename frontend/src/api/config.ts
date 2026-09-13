/**
 * URL base de la API. Se puede sobrescribir con la variable de entorno
 * EXPO_PUBLIC_API_URL (Expo la incrusta en el bundle al compilar), definiéndola en
 * un archivo .env.local (no se sube al repo). Ejemplos:
 *  - Emulador Android (AVD):        EXPO_PUBLIC_API_URL=http://10.0.2.2:8080
 *  - Dispositivo físico / Expo Go:  EXPO_PUBLIC_API_URL=http://<ip-lan-de-tu-pc>:8080
 * Sin la variable, apunta a producción.
 */
const DEFAULT_API_URL = "https://moteros.deveps.dev";

export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? DEFAULT_API_URL).replace(/\/+$/, "");

/**
 * Las imágenes subidas llegan como ruta relativa ("/uploads/xxx.png").
 * Devuelve la URL absoluta para mostrarlas; deja intactas las URLs ya absolutas.
 */
export function mediaUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_URL}${path.startsWith("/") ? "" : "/"}${path}`;
}
