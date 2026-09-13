import { useTheme } from "@/theme";
import { View } from "react-native";
import { Chip } from "./ui/Chip";
import { Text } from "./ui/Text";

interface OptionPickerProps<T extends string> {
  label: string;
  options: { value: T; label: string }[];
  value: T | undefined;
  onChange: (value: T) => void;
}

/** Selector de una opción entre varias, con chips. */
export function OptionPicker<T extends string>({ label, options, value, onChange }: OptionPickerProps<T>) {
  const theme = useTheme();
  return (
    <View style={{ gap: theme.spacing.sm }}>
      <Text variant="overline" color="inkFaint">
        {label}
      </Text>
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: theme.spacing.sm }}>
        {options.map((o) => (
          <Chip key={o.value} label={o.label} selected={o.value === value} onPress={() => onChange(o.value)} />
        ))}
      </View>
    </View>
  );
}
