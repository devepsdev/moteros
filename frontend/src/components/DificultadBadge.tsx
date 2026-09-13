import { etiquetaDificultad } from "@/lib/format";
import type { Dificultad } from "@/types/dto";
import { useTheme } from "@/theme";
import { View } from "react-native";
import { Text } from "./ui/Text";

export function DificultadBadge({ dificultad }: { dificultad: Dificultad }) {
  const theme = useTheme();
  const color = {
    facil: theme.colors.support,
    moderada: theme.colors.amber,
    dificil: theme.colors.accent,
    extrema: theme.colors.danger,
  }[dificultad];

  return (
    <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
      <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: color }} />
      <Text variant="captionMedium" color="inkMuted">
        {etiquetaDificultad(dificultad)}
      </Text>
    </View>
  );
}
