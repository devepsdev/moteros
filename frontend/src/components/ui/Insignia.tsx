import { useTheme } from "@/theme";
import { View, type ViewStyle } from "react-native";
import { Text } from "./Text";

/** Contador redondo (no leídos). No se pinta si el valor es 0. */
export function Insignia({ valor, style }: { valor: number; style?: ViewStyle }) {
  const theme = useTheme();
  if (!valor) return null;
  return (
    <View
      pointerEvents="none"
      style={[
        {
          minWidth: 18,
          height: 18,
          paddingHorizontal: 5,
          borderRadius: 9,
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: theme.colors.accent,
          borderWidth: 2,
          borderColor: theme.colors.background,
        },
        style,
      ]}
    >
      <Text variant="caption" style={{ color: "#FFFFFF", fontSize: 10, lineHeight: 12 }}>
        {valor > 99 ? "99+" : valor}
      </Text>
    </View>
  );
}
