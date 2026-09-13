import { useTheme, type TypographyVariant } from "@/theme";
import { Text as RNText, type TextProps as RNTextProps } from "react-native";

type ColorToken = "ink" | "inkMuted" | "inkFaint" | "inkInverse" | "accent" | "support" | "amber" | "danger";

export interface TextProps extends RNTextProps {
  variant?: TypographyVariant;
  color?: ColorToken;
  center?: boolean;
}

export function Text({ variant = "body", color = "ink", center, style, ...rest }: TextProps) {
  const theme = useTheme();
  return (
    <RNText
      style={[theme.typography[variant], { color: theme.colors[color] }, center && { textAlign: "center" }, style]}
      {...rest}
    />
  );
}
