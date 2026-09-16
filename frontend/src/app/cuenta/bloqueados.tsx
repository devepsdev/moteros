import * as bloqueosApi from "@/api/bloqueos";
import { UsuarioFila } from "@/components/UsuarioFila";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Alert, FlatList, RefreshControl, View } from "react-native";

export default function BloqueadosScreen() {
  const theme = useTheme();
  const router = useRouter();
  const lista = usePagedList((page) => bloqueosApi.misBloqueados(page), []);
  const [ocupado, setOcupado] = useState<string | null>(null);

  const desbloquear = async (uuid: string) => {
    setOcupado(uuid);
    try {
      await bloqueosApi.desbloquear(uuid);
      lista.updateItems((items) => items.filter((u) => u.uuid !== uuid));
    } catch (cause) {
      Alert.alert("No se ha podido desbloquear", describeError(cause).message);
    } finally {
      setOcupado(null);
    }
  };

  return (
    <Screen>
      <IconButton
        name="arrow-left"
        accessibilityLabel="Volver"
        onPress={() => (router.canGoBack() ? router.back() : router.replace("/perfil"))}
        style={{ marginLeft: theme.spacing.sm }}
      />

      <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.xs, marginBottom: theme.spacing.md }}>
        <Text variant="overline" color="accent">
          Cuenta
        </Text>
        <Text variant="display">Bloqueados</Text>
        <Text variant="body" color="inkMuted">
          No podéis escribiros ni ser amigos, y no veis las publicaciones ni los comentarios del otro.
        </Text>
      </View>

      {lista.loading && lista.items.length === 0 ? (
        <LoadingView />
      ) : lista.error && lista.items.length === 0 ? (
        <ErrorState onRetry={lista.reload} />
      ) : (
        <FlatList
          data={lista.items}
          keyExtractor={(u) => u.uuid}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, flexGrow: 1 }}
          onEndReached={lista.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={lista.loading && lista.items.length > 0} onRefresh={lista.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          renderItem={({ item }) => (
            <UsuarioFila usuario={item}>
              <Button label="Desbloquear" variant="ghost" disabled={ocupado === item.uuid} onPress={() => desbloquear(item.uuid)} />
            </UsuarioFila>
          )}
          ListEmptyComponent={<EmptyState icon="slash" title="No has bloqueado a nadie" message="Puedes bloquear a un usuario desde su perfil o desde una conversación." />}
          ListFooterComponent={<ListFooter loadingMore={lista.loadingMore} error={lista.items.length > 0 ? lista.error : null} />}
        />
      )}
    </Screen>
  );
}
