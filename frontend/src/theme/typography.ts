import type { TextStyle } from "react-native";

/**
 * Barlow Condensed (condensada, con aire de dorsal y de cartel de carretera) para
 * titulares; Inter para todo lo demás. Los nombres coinciden con las claves que
 * registra useFonts en el layout raíz.
 */
export const fontFamily = {
  display: "BarlowCondensed_700Bold",
  displaySemibold: "BarlowCondensed_600SemiBold",
  sans: "Inter_400Regular",
  sansMedium: "Inter_500Medium",
  sansSemibold: "Inter_600SemiBold",
} as const;

export type TypographyVariant =
  | "display"
  | "title1"
  | "title2"
  | "title3"
  | "body"
  | "bodyMedium"
  | "caption"
  | "captionMedium"
  /** Microtexto en mayúsculas y espaciado. */
  | "overline"
  | "button"
  /** Cifras destacadas (km, minutos, contadores). */
  | "stat";

export const typography: Record<TypographyVariant, TextStyle> = {
  display: { fontFamily: fontFamily.display, fontSize: 38, lineHeight: 40, letterSpacing: 0.2, textTransform: "uppercase" },
  title1: { fontFamily: fontFamily.display, fontSize: 28, lineHeight: 32, letterSpacing: 0.2 },
  title2: { fontFamily: fontFamily.displaySemibold, fontSize: 22, lineHeight: 26, letterSpacing: 0.2 },
  title3: { fontFamily: fontFamily.displaySemibold, fontSize: 19, lineHeight: 23, letterSpacing: 0.2 },
  body: { fontFamily: fontFamily.sans, fontSize: 15, lineHeight: 22 },
  bodyMedium: { fontFamily: fontFamily.sansMedium, fontSize: 15, lineHeight: 22 },
  caption: { fontFamily: fontFamily.sans, fontSize: 13, lineHeight: 18 },
  captionMedium: { fontFamily: fontFamily.sansMedium, fontSize: 13, lineHeight: 18 },
  overline: { fontFamily: fontFamily.sansSemibold, fontSize: 11, lineHeight: 14, letterSpacing: 1.4, textTransform: "uppercase" },
  button: { fontFamily: fontFamily.sansSemibold, fontSize: 15, lineHeight: 20, letterSpacing: 0.2 },
  stat: { fontFamily: fontFamily.display, fontSize: 26, lineHeight: 28 },
};
