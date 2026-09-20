import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Pressable, type ViewStyle } from "react-native";
import { Text } from "./Text";

interface ChipProps {
  label: string;
  selected?: boolean;
  /** Icono opcional a la izquierda de la etiqueta. */
  icon?: React.ComponentProps<typeof Feather>["name"];
  onPress?: () => void;
  style?: ViewStyle;
}

/** Etiqueta seleccionable para filtros y selectores de opciones. */
export function Chip({ label, selected, icon, onPress, style }: ChipProps) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? "button" : undefined}
      accessibilityState={{ selected }}
      style={({ pressed }) => [
        {
          paddingHorizontal: theme.spacing.md,
          height: 34,
          flexDirection: "row",
          alignItems: "center",
          gap: 6,
          justifyContent: "center",
          borderRadius: theme.radius.full,
          borderWidth: 1,
          borderColor: selected ? theme.colors.accent : theme.colors.border,
          backgroundColor: selected ? theme.colors.accentSoft : theme.colors.surface,
          opacity: pressed ? 0.7 : 1,
        },
        style,
      ]}
    >
      {icon ? <Feather name={icon} size={13} color={selected ? theme.colors.accent : theme.colors.inkMuted} /> : null}
      <Text variant="captionMedium" color={selected ? "accent" : "inkMuted"}>
        {label}
      </Text>
    </Pressable>
  );
}
