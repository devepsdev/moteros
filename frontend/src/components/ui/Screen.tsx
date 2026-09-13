import { useTheme } from "@/theme";
import type { ReactNode } from "react";
import { View, type ViewStyle } from "react-native";
import { SafeAreaView, type Edge } from "react-native-safe-area-context";

interface ScreenProps {
  children: ReactNode;
  /** Bordes donde respetar el área segura. Por defecto, solo arriba. */
  edges?: readonly Edge[];
  /** Añade el margen horizontal estándar de pantalla. */
  padded?: boolean;
  style?: ViewStyle;
}

export function Screen({ children, edges = ["top"], padded, style }: ScreenProps) {
  const theme = useTheme();
  return (
    <SafeAreaView edges={edges} style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <View style={[{ flex: 1 }, padded && { paddingHorizontal: theme.screenPadding }, style]}>{children}</View>
    </SafeAreaView>
  );
}
