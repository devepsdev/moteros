import { API_URL } from "@/api/config";
import * as WebBrowser from "expo-web-browser";

/** Páginas legales públicas, servidas por la API (las mismas que enlaza la ficha de Google Play). */
export type PaginaLegal = "privacidad" | "terminos" | "eliminar-cuenta";

export function abrirLegal(pagina: PaginaLegal) {
  WebBrowser.openBrowserAsync(`${API_URL}/${pagina}`).catch(() => {});
}
