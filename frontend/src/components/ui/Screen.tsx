import { useTheme } from "@/theme";
import { LinearGradient } from "expo-linear-gradient";
import type { ReactNode } from "react";
import { View, type ViewStyle } from "react-native";
import { SafeAreaView, type Edge } from "react-native-safe-area-context";

interface ScreenProps {
  children: ReactNode;
  /** Bordes donde respetar el área segura. Por defecto, solo arriba. */
  edges?: readonly Edge[];
  /** Añade el margen horizontal estándar de pantalla. */
  padded?: boolean;
  /** Quita el degradado de fondo (mapas a pantalla completa, por ejemplo). */
  sinDegradado?: boolean;
  style?: ViewStyle;
}

export function Screen({ children, edges = ["top"], padded, sinDegradado, style }: ScreenProps) {
  const theme = useTheme();
  return (
    <SafeAreaView edges={edges} style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {/* Un halo naranja muy tenue arriba: da profundidad al fondo sin restar contraste. */}
      {sinDegradado ? null : (
        <LinearGradient
          pointerEvents="none"
          colors={[theme.isDark ? "rgba(255, 106, 19, 0.13)" : "rgba(232, 90, 12, 0.10)", "rgba(255, 106, 19, 0)"]}
          style={{ position: "absolute", top: 0, left: 0, right: 0, height: 280 }}
        />
      )}
      <View style={[{ flex: 1 }, padded && { paddingHorizontal: theme.screenPadding }, style]}>{children}</View>
    </SafeAreaView>
  );
}
