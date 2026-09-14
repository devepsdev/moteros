import * as publicacionesApi from "@/api/publicaciones";
import { PublicacionCard } from "@/components/PublicacionCard";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { Insignia } from "@/components/ui/Insignia";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { useNotificacionesNoLeidas } from "@/lib/noLeidos";
import { useAlternarLike } from "@/lib/useAlternarLike";
import { usePagedList } from "@/lib/usePagedList";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import { useRouter } from "expo-router";
import { FlatList, RefreshControl, View } from "react-native";

export default function FeedScreen() {
  const theme = useTheme();
  const router = useRouter();
  const feed = usePagedList((page) => publicacionesApi.feed(page), []);
  const alternarLike = useAlternarLike(feed.updateItems);
  const notificaciones = useNotificacionesNoLeidas();

  // Al volver de publicar o de un detalle se refresca el feed.
  useRefocus(feed.reload);

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md }}>
        <Text variant="display" style={{ fontSize: 30 }}>
          moter<Text variant="display" color="accent" style={{ fontSize: 30 }}>@</Text>s
        </Text>
        <View style={{ flexDirection: "row", gap: theme.spacing.sm }}>
          <IconButton name="search" variant="surface" size={18} accessibilityLabel="Buscar moteros" onPress={() => router.push("/buscar")} />
          <View>
            <IconButton name="bell" variant="surface" size={18} accessibilityLabel={notificaciones ? `Notificaciones, ${notificaciones} sin leer` : "Notificaciones"} onPress={() => router.push("/notificaciones")} />
            <Insignia valor={notificaciones} style={{ position: "absolute", top: -2, right: -2 }} />
          </View>
          <IconButton name="edit-3" variant="accent" size={18} accessibilityLabel="Nueva publicación" onPress={() => router.push("/publicacion/nueva")} />
        </View>
      </View>

      {feed.loading && feed.items.length === 0 ? (
        <LoadingView />
      ) : feed.error && feed.items.length === 0 ? (
        <ErrorState onRetry={feed.reload} />
      ) : (
        <FlatList
          data={feed.items}
          keyExtractor={(p) => p.uuid}
          renderItem={({ item }) => <PublicacionCard publicacion={item} onLike={() => alternarLike(item)} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.sm, gap: theme.spacing.lg, flexGrow: 1 }}
          onEndReached={feed.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={feed.loading && feed.items.length > 0} onRefresh={feed.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          ListEmptyComponent={
            <EmptyState
              icon="radio"
              title="Tu feed está en silencio"
              message="Aquí verás lo que publicáis tú y tus amigos. Cuenta tu última salida o comparte una ruta."
              actionLabel="Publicar algo"
              onAction={() => router.push("/publicacion/nueva")}
            />
          }
          ListFooterComponent={<ListFooter loadingMore={feed.loadingMore} error={feed.items.length > 0 ? feed.error : null} />}
        />
      )}
    </Screen>
  );
}
