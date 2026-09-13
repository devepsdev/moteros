/**
 * Paleta "asfalto y naranja": fondos de asfalto oscuro, naranja intenso de acento
 * (el de los intermitentes y los monos de cuero) y un verde gasolina de apoyo.
 * El tema oscuro es el principal; el claro mantiene la misma identidad.
 */

export interface ThemeColors {
  /** Fondo general de pantalla. */
  background: string;
  /** Tarjetas y superficies elevadas. */
  surface: string;
  /** Superficies hundidas: campos de texto, skeletons, celdas de info. */
  surfaceSunken: string;
  /** Texto principal. */
  ink: string;
  /** Texto secundario. */
  inkMuted: string;
  /** Texto terciario, metadatos. */
  inkFaint: string;
  /** Texto sobre el acento. */
  inkInverse: string;
  /** Líneas de 1px, separadores, bordes de tarjeta. */
  border: string;
  /** Bordes con más presencia (inputs enfocados, chips activos). */
  borderStrong: string;
  /** Acento principal: naranja. */
  accent: string;
  /** Fondo tintado del acento, para chips y estados seleccionados. */
  accentSoft: string;
  /** Apoyo: verde gasolina (éxito, dificultad fácil). */
  support: string;
  supportSoft: string;
  /** Ámbar: estrellas de valoración y avisos. */
  amber: string;
  /** Errores y acciones destructivas. */
  danger: string;
  dangerSoft: string;
  /** Color del trazo del track sobre el mapa. */
  track: string;
  /** Velo oscuro sobre fotografías. */
  scrimFrom: string;
  scrimTo: string;
}

export const darkColors: ThemeColors = {
  background: "#0E0F11",
  surface: "#17191C",
  surfaceSunken: "#1F2226",
  ink: "#F4F2EE",
  inkMuted: "#A6A29B",
  inkFaint: "#6F6C66",
  inkInverse: "#111111",
  border: "#262A2F",
  borderStrong: "#3A3F46",
  accent: "#FF6A13",
  accentSoft: "#2B1A10",
  support: "#4FB286",
  supportSoft: "#12241C",
  amber: "#F5B82E",
  danger: "#FF5A4F",
  dangerSoft: "#2A1413",
  track: "#FF6A13",
  scrimFrom: "rgba(0, 0, 0, 0)",
  scrimTo: "rgba(0, 0, 0, 0.88)",
};

export const lightColors: ThemeColors = {
  background: "#F5F4F1",
  surface: "#FFFFFF",
  surfaceSunken: "#ECEAE6",
  ink: "#16181B",
  inkMuted: "#5E5B56",
  inkFaint: "#8E8A84",
  inkInverse: "#FFFFFF",
  border: "#E1DED9",
  borderStrong: "#C9C5BE",
  accent: "#E85A0C",
  accentSoft: "#FDEBDF",
  support: "#23845C",
  supportSoft: "#E1F1E9",
  amber: "#C98A00",
  danger: "#C8352B",
  dangerSoft: "#FBE7E5",
  track: "#E85A0C",
  scrimFrom: "rgba(0, 0, 0, 0)",
  scrimTo: "rgba(0, 0, 0, 0.8)",
};
