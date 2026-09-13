import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Pressable, View } from "react-native";

/** Estrellas de solo lectura (valoración media). */
export function Stars({ value, size = 13 }: { value: number | null | undefined; size?: number }) {
  const theme = useTheme();
  const redondeado = Math.round(value ?? 0);
  return (
    <View style={{ flexDirection: "row", gap: 2 }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Feather key={n} name="star" size={size} color={n <= redondeado ? theme.colors.amber : theme.colors.borderStrong} />
      ))}
    </View>
  );
}

/** Selector de 1 a 5 estrellas. */
export function StarPicker({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: "row", gap: theme.spacing.md, justifyContent: "center" }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Pressable key={n} onPress={() => onChange(n)} hitSlop={6} accessibilityRole="button" accessibilityLabel={`${n} estrellas`}>
          <Feather name="star" size={36} color={n <= value ? theme.colors.amber : theme.colors.borderStrong} />
        </Pressable>
      ))}
    </View>
  );
}
