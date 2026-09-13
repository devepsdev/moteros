import { useColorScheme } from "react-native";
import { darkColors, lightColors, type ThemeColors } from "./colors";
import { radius, screenPadding, shadow, spacing } from "./layout";
import { fontFamily, typography } from "./typography";

export interface Theme {
  colors: ThemeColors;
  isDark: boolean;
  spacing: typeof spacing;
  radius: typeof radius;
  shadow: typeof shadow;
  typography: typeof typography;
  fontFamily: typeof fontFamily;
  screenPadding: typeof screenPadding;
}

const base = { spacing, radius, shadow, typography, fontFamily, screenPadding };

export const lightTheme: Theme = { ...base, colors: lightColors, isDark: false };
export const darkTheme: Theme = { ...base, colors: darkColors, isDark: true };

/** El tema oscuro es el de la identidad; solo se usa el claro si el sistema lo pide explícitamente. */
export function useTheme(): Theme {
  return useColorScheme() === "light" ? lightTheme : darkTheme;
}

export { darkColors, fontFamily, lightColors, radius, screenPadding, shadow, spacing, typography };
export type { ThemeColors };
export type { TypographyVariant } from "./typography";
