import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Pressable, type ViewStyle } from "react-native";

type Variant = "plain" | "surface" | "floating" | "accent";

interface IconButtonProps {
  name: React.ComponentProps<typeof Feather>["name"];
  onPress?: () => void;
  variant?: Variant;
  size?: number;
  color?: string;
  accessibilityLabel: string;
  disabled?: boolean;
  style?: ViewStyle;
}

export function IconButton({ name, onPress, variant = "plain", size = 20, color, accessibilityLabel, disabled, style }: IconButtonProps) {
  const theme = useTheme();
  const diameter = size + 22;

  const iconColor =
    color ?? (variant === "accent" ? "#FFFFFF" : variant === "floating" ? "#FFFFFF" : theme.colors.ink);

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      hitSlop={8}
      style={({ pressed }) => [
        {
          width: diameter,
          height: diameter,
          alignItems: "center",
          justifyContent: "center",
          borderRadius: theme.radius.full,
          opacity: disabled ? 0.4 : pressed ? 0.6 : 1,
        },
        variant === "surface" && { backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border },
        variant === "floating" && { backgroundColor: "rgba(14, 15, 17, 0.72)" },
        variant === "accent" && { backgroundColor: theme.colors.accent, ...theme.shadow.raised },
        style,
      ]}
    >
      <Feather name={name} size={size} color={iconColor} />
    </Pressable>
  );
}
