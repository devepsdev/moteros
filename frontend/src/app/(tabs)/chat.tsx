import * as chatApi from "@/api/chat";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { formatRelativo } from "@/lib/format";
import { refrescarNoLeidos } from "@/lib/noLeidos";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import type { Conversacion } from "@/types/dto";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useRef } from "react";
import { FlatList, Pressable, RefreshControl, View } from "react-native";

const INTERVALO_MS = 15_000;

export default function ChatScreen() {
  const theme = useTheme();
  const router = useRouter();
  const conversaciones = usePagedList((page) => chatApi.conversaciones(page), []);

  // Mientras la pestaña está visible, la lista se refresca sola (no hay push).
  const { reload } = conversaciones;
  const primeraVez = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (primeraVez.current) primeraVez.current = false;
      else reload();
      refrescarNoLeidos();
      const id = setInterval(reload, INTERVALO_MS);
      return () => clearInterval(id);
    }, [reload])
  );

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md }}>
        <View>
          <Text variant="overline" color="accent">
            Mensajes
          </Text>
          <Text variant="display">Chat</Text>
        </View>
        <IconButton name="users" variant="surface" accessibilityLabel="Amigos" onPress={() => router.push("/amigos")} />
      </View>

      {conversaciones.loading && conversaciones.items.length === 0 ? (
        <LoadingView />
      ) : conversaciones.error && conversaciones.items.length === 0 ? (
        <ErrorState onRetry={conversaciones.reload} />
      ) : (
        <FlatList
          data={conversaciones.items}
          keyExtractor={(c) => c.uuid}
          renderItem={({ item }) => <ConversacionFila conversacion={item} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, flexGrow: 1 }}
          ItemSeparatorComponent={() => <View style={{ height: 1, backgroundColor: theme.colors.border, marginLeft: 64 }} />}
          onEndReached={conversaciones.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={false} onRefresh={conversaciones.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          ListEmptyComponent={
            <EmptyState
              icon="message-circle"
              title="Sin conversaciones"
              message="Escribe a tus amigos desde su perfil para organizar la próxima salida."
              actionLabel="Ver amigos"
              onAction={() => router.push("/amigos")}
            />
          }
          ListFooterComponent={<ListFooter loadingMore={conversaciones.loadingMore} error={conversaciones.items.length > 0 ? conversaciones.error : null} />}
        />
      )}
    </Screen>
  );
}

function ConversacionFila({ conversacion: c }: { conversacion: Conversacion }) {
  const theme = useTheme();
  const router = useRouter();
  const noLeidos = c.numNoLeidos ?? 0;
  const ultimo = c.ultimoMensaje;

  return (
    <Pressable
      onPress={() =>
        router.push({
          pathname: "/chat/[uuid]",
          params: { uuid: c.uuid, nombre: c.interlocutor.nombreCompleto, usuarioUuid: c.interlocutor.uuid, foto: c.interlocutor.fotoPerfilUrl ?? undefined },
        })
      }
      style={({ pressed }) => ({ flexDirection: "row", alignItems: "center", gap: theme.spacing.md, paddingVertical: theme.spacing.md, opacity: pressed ? 0.7 : 1 })}
    >
      <Avatar nombre={c.interlocutor.nombreCompleto} fotoUrl={c.interlocutor.fotoPerfilUrl} size={50} />
      <View style={{ flex: 1, gap: 2 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
          <Text variant={noLeidos ? "bodyMedium" : "body"} numberOfLines={1} style={{ flex: 1 }}>
            {c.interlocutor.nombreCompleto}
          </Text>
          <Text variant="caption" color={noLeidos ? "accent" : "inkFaint"}>
            {formatRelativo(ultimo?.fechaEnvio ?? c.fechaCreacion)}
          </Text>
        </View>
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
          <Text variant="caption" color={noLeidos ? "ink" : "inkMuted"} numberOfLines={1} style={{ flex: 1 }}>
            {ultimo ? `${ultimo.propio ? "Tú: " : ""}${ultimo.contenido}` : "Sin mensajes"}
          </Text>
          {noLeidos ? (
            <View style={{ minWidth: 20, height: 20, paddingHorizontal: 6, borderRadius: 10, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.accent }}>
              <Text variant="caption" style={{ color: "#FFFFFF", fontSize: 11, lineHeight: 14 }}>
                {noLeidos > 99 ? "99+" : noLeidos}
              </Text>
            </View>
          ) : null}
        </View>
      </View>
    </Pressable>
  );
}
