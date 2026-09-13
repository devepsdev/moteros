import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { View } from "react-native";
import { Button } from "./Button";
import { Text } from "./Text";

interface EmptyStateProps {
  icon?: React.ComponentProps<typeof Feather>["name"];
  title: string;
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ icon = "compass", title, message, actionLabel, onAction }: EmptyStateProps) {
  const theme = useTheme();

  return (
    <View style={{ alignItems: "center", justifyContent: "center", paddingVertical: theme.spacing.huge, paddingHorizontal: theme.screenPadding, gap: theme.spacing.md }}>
      <View
        style={{
          width: 64,
          height: 64,
          borderRadius: theme.radius.full,
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: theme.colors.accentSoft,
        }}
      >
        <Feather name={icon} size={26} color={theme.colors.accent} />
      </View>
      <Text variant="title2" center>
        {title}
      </Text>
      {message ? (
        <Text variant="body" color="inkMuted" center style={{ maxWidth: 300 }}>
          {message}
        </Text>
      ) : null}
      {actionLabel && onAction ? (
        <Button label={actionLabel} onPress={onAction} variant="secondary" style={{ marginTop: theme.spacing.sm }} />
      ) : null}
    </View>
  );
}

/** Estado de error de carga con botón de reintento. */
export function ErrorState({ message, onRetry }: { message?: string; onRetry: () => void }) {
  return (
    <EmptyState
      icon="wifi-off"
      title="No se ha podido cargar"
      message={message ?? "Revisa tu conexión e inténtalo de nuevo."}
      actionLabel="Reintentar"
      onAction={onRetry}
    />
  );
}
