import * as notificacionesApi from "@/api/notificaciones";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { formatRelativo } from "@/lib/format";
import { refrescarNoLeidos, vaciarNotificaciones } from "@/lib/noLeidos";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import type { Notificacion, TipoNotificacion } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useRouter, type Href } from "expo-router";
import { Pressable, FlatList, RefreshControl, View } from "react-native";

type FeatherName = React.ComponentProps<typeof Feather>["name"];

const ICONOS: Record<TipoNotificacion, FeatherName> = {
  like: "heart",
  comentario: "message-square",
  solicitud_amistad: "user-plus",
  amistad_aceptada: "user-check",
  inscripcion_quedada: "calendar",
  nueva_quedada: "calendar",
  quedada_cancelada: "x-circle",
  mensaje: "message-circle",
  valoracion_ruta: "star",
};

/** Pantalla a la que lleva cada tipo; null si lo referenciado ya no existe. */
function destino(n: Notificacion): Href | null {
  const ref = n.referenciaUuid;
  switch (n.tipo) {
    case "like":
    case "comentario":
      return ref ? { pathname: "/publicacion/[uuid]", params: { uuid: ref } } : null;
    case "inscripcion_quedada":
    case "nueva_quedada":
    case "quedada_cancelada":
      return ref ? { pathname: "/quedada/[uuid]", params: { uuid: ref } } : null;
    case "valoracion_ruta":
      return ref ? { pathname: "/ruta/[uuid]", params: { uuid: ref } } : null;
    case "mensaje":
      return ref
        ? {
            pathname: "/chat/[uuid]",
            params: { uuid: ref, nombre: n.usuarioOrigen?.nombreCompleto, usuarioUuid: n.usuarioOrigen?.uuid, foto: n.usuarioOrigen?.fotoPerfilUrl ?? undefined },
          }
        : null;
    case "solicitud_amistad":
      return "/amigos";
    case "amistad_aceptada":
      return n.usuarioOrigen ? { pathname: "/usuario/[uuid]", params: { uuid: n.usuarioOrigen.uuid } } : null;
  }
}

export default function NotificacionesScreen() {
  const theme = useTheme();
  const router = useRouter();
  const lista = usePagedList((page) => notificacionesApi.listar(page), []);
  const hayNoLeidas = lista.items.some((n) => !n.leido);

  const abrir = (n: Notificacion) => {
    if (!n.leido) {
      lista.updateItems((items) => items.map((x) => (x.uuid === n.uuid ? { ...x, leido: true } : x)));
      notificacionesApi
        .marcarLeida(n.uuid)
        .then(refrescarNoLeidos)
        .catch(() => {});
    }
    const href = destino(n);
    if (href) router.push(href);
  };

  const marcarTodas = () => {
    lista.updateItems((items) => items.map((x) => ({ ...x, leido: true })));
    vaciarNotificaciones();
    notificacionesApi.marcarTodasLeidas().catch(() => refrescarNoLeidos());
  };

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.spacing.sm }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={() => (router.canGoBack() ? router.back() : router.replace("/"))} />
        {hayNoLeidas ? (
          <Pressable onPress={marcarTodas} hitSlop={8} style={({ pressed }) => ({ paddingHorizontal: theme.spacing.md, opacity: pressed ? 0.6 : 1 })}>
            <Text variant="captionMedium" color="accent">
              Marcar todas como leídas
            </Text>
          </Pressable>
        ) : null}
      </View>

      <View style={{ paddingHorizontal: theme.screenPadding, marginBottom: theme.spacing.md, gap: theme.spacing.xs }}>
        <Text variant="overline" color="accent">
          Actividad
        </Text>
        <Text variant="display">Notificaciones</Text>
      </View>

      {lista.loading && lista.items.length === 0 ? (
        <LoadingView />
      ) : lista.error && lista.items.length === 0 ? (
        <ErrorState onRetry={lista.reload} />
      ) : (
        <FlatList
          data={lista.items}
          keyExtractor={(n) => n.uuid}
          contentContainerStyle={{ flexGrow: 1 }}
          onEndReached={lista.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={
            <RefreshControl
              refreshing={lista.loading && lista.items.length > 0}
              onRefresh={() => {
                lista.reload();
                refrescarNoLeidos();
              }}
              tintColor={theme.colors.accent}
              colors={[theme.colors.accent]}
              progressBackgroundColor={theme.colors.surface}
            />
          }
          renderItem={({ item }) => <Fila notificacion={item} onPress={() => abrir(item)} />}
          ListEmptyComponent={<EmptyState icon="bell" title="Todo tranquilo" message="Aquí verás los me gusta, comentarios, solicitudes y avisos de quedadas." />}
          ListFooterComponent={<ListFooter loadingMore={lista.loadingMore} error={lista.items.length > 0 ? lista.error : null} />}
        />
      )}
    </Screen>
  );
}

function Fila({ notificacion: n, onPress }: { notificacion: Notificacion; onPress: () => void }) {
  const theme = useTheme();
  const cancelada = n.tipo === "quedada_cancelada";

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        flexDirection: "row",
        alignItems: "center",
        gap: theme.spacing.md,
        paddingHorizontal: theme.screenPadding,
        paddingVertical: theme.spacing.md,
        backgroundColor: pressed ? theme.colors.surfaceSunken : n.leido ? "transparent" : theme.colors.accentSoft,
      })}
    >
      <View>
        {n.usuarioOrigen ? (
          <Avatar nombre={n.usuarioOrigen.nombreCompleto} fotoUrl={n.usuarioOrigen.fotoPerfilUrl} size={44} />
        ) : (
          <View style={{ width: 44, height: 44, borderRadius: 22, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.surface }}>
            <Feather name="bell" size={18} color={theme.colors.inkMuted} />
          </View>
        )}
        <View
          style={{
            position: "absolute",
            right: -4,
            bottom: -4,
            width: 22,
            height: 22,
            borderRadius: 11,
            alignItems: "center",
            justifyContent: "center",
            backgroundColor: cancelada ? theme.colors.danger : theme.colors.accent,
            borderWidth: 2,
            borderColor: theme.colors.background,
          }}
        >
          <Feather name={ICONOS[n.tipo]} size={11} color="#FFFFFF" />
        </View>
      </View>
      <View style={{ flex: 1, gap: 2 }}>
        <Text variant={n.leido ? "body" : "bodyMedium"} numberOfLines={3}>
          {n.mensaje}
        </Text>
        <Text variant="caption" color="inkFaint">
          {formatRelativo(n.fechaCreacion)}
        </Text>
      </View>
      {!n.leido ? <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: theme.colors.accent }} /> : null}
    </Pressable>
  );
}
