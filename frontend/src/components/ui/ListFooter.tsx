import { useTheme } from "@/theme";
import { ActivityIndicator, View } from "react-native";
import { Text } from "./Text";

/** Pie de las listas paginadas: indicador de "cargando más" o aviso de error al paginar. */
export function ListFooter({ loadingMore, error }: { loadingMore: boolean; error: Error | null }) {
  const theme = useTheme();
  if (loadingMore) {
    return (
      <View style={{ paddingVertical: theme.spacing.xl }}>
        <ActivityIndicator color={theme.colors.accent} />
      </View>
    );
  }
  if (error) {
    return (
      <Text variant="caption" color="inkMuted" center style={{ paddingVertical: theme.spacing.xl }}>
        No se han podido cargar más resultados.
      </Text>
    );
  }
  return <View style={{ height: theme.spacing.huge }} />;
}

export function LoadingView() {
  const theme = useTheme();
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", paddingVertical: theme.spacing.huge }}>
      <ActivityIndicator color={theme.colors.accent} size="large" />
    </View>
  );
}
