import { useTheme } from "@/theme";
import { Pressable, View, type ViewStyle } from "react-native";
import { Text } from "./Text";

interface SegmentedProps<T extends string> {
  options: { value: T; label: string; badge?: number }[];
  value: T;
  onChange: (value: T) => void;
  style?: ViewStyle;
}

/** Selector de pestañas dentro de una pantalla. */
export function Segmented<T extends string>({ options, value, onChange, style }: SegmentedProps<T>) {
  const theme = useTheme();
  return (
    <View
      style={[
        {
          flexDirection: "row",
          padding: 3,
          borderRadius: theme.radius.md,
          backgroundColor: theme.colors.surfaceSunken,
          borderWidth: 1,
          borderColor: theme.colors.border,
        },
        style,
      ]}
    >
      {options.map((o) => {
        const activo = o.value === value;
        return (
          <Pressable
            key={o.value}
            onPress={() => onChange(o.value)}
            accessibilityRole="tab"
            accessibilityState={{ selected: activo }}
            style={{
              flex: 1,
              height: 36,
              flexDirection: "row",
              alignItems: "center",
              justifyContent: "center",
              gap: 6,
              borderRadius: theme.radius.sm,
              backgroundColor: activo ? theme.colors.surface : "transparent",
            }}
          >
            <Text variant="captionMedium" color={activo ? "ink" : "inkMuted"} numberOfLines={1}>
              {o.label}
            </Text>
            {o.badge ? (
              <View style={{ minWidth: 18, height: 18, paddingHorizontal: 5, borderRadius: 9, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.accent }}>
                <Text variant="caption" style={{ color: "#FFFFFF", fontSize: 11, lineHeight: 14 }}>
                  {o.badge > 99 ? "99+" : o.badge}
                </Text>
              </View>
            ) : null}
          </Pressable>
        );
      })}
    </View>
  );
}
