import { ApiError } from "@/api/client";
import * as publicacionesApi from "@/api/publicaciones";
import { useAuth } from "@/auth/AuthContext";
import { PublicacionCard } from "@/components/PublicacionCard";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { abrirDenuncia } from "@/lib/denuncia";
import { describeError } from "@/lib/errors";
import { formatRelativo } from "@/lib/format";
import { useAlternarLike } from "@/lib/useAlternarLike";
import { useAsync } from "@/lib/useAsync";
import { usePagedList } from "@/lib/usePagedList";
import { useDesplazamientoTeclado } from "@/lib/useTeclado";
import { useTheme } from "@/theme";
import type { Comentario, Publicacion } from "@/types/dto";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Alert, FlatList, Pressable, TextInput, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function PublicacionDetalleScreen() {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const teclado = useDesplazamientoTeclado();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { user } = useAuth();

  const carga = useAsync(() => publicacionesApi.obtener(uuid), [uuid]);
  const comentarios = usePagedList((page) => publicacionesApi.comentarios(uuid, page), [uuid]);

  // Copia local para poder dar like y contar comentarios sin volver a pedirla.
  const [local, setLocal] = useState<Publicacion | null>(null);
  const publicacion = local ?? carga.data;
  const actualizar = (fn: (p: Publicacion) => Publicacion) =>
    setLocal((previa) => {
      const base = previa ?? carga.data;
      return base ? fn(base) : previa;
    });

  const alternarLike = useAlternarLike((updater) => actualizar((p) => updater([p])[0]));

  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/"));

  const comentar = async () => {
    const contenido = texto.trim();
    if (!contenido) return;
    setEnviando(true);
    try {
      const nuevo = await publicacionesApi.comentar(uuid, contenido);
      setTexto("");
      comentarios.updateItems((items) => [...items, nuevo]);
      actualizar((p) => ({ ...p, numComentarios: (p.numComentarios ?? 0) + 1 }));
    } catch (cause) {
      Alert.alert("No se ha podido comentar", describeError(cause).message);
    } finally {
      setEnviando(false);
    }
  };

  const opcionesComentario = (c: Comentario) => {
    const propio = c.autor.uuid === user?.uuid;
    const puedeBorrar = propio || publicacion?.autor.uuid === user?.uuid;
    Alert.alert("Comentario", undefined, [
      { text: "Cancelar", style: "cancel" },
      ...(propio ? [] : [{ text: "Denunciar", onPress: () => abrirDenuncia(router, "comentario", c.uuid, { uuid: c.autor.uuid, nombre: c.autor.nombreCompleto }) }]),
      ...(puedeBorrar ? [{ text: "Eliminar", style: "destructive" as const, onPress: () => eliminarComentario(c) }] : []),
    ]);
  };

  const eliminarComentario = (c: Comentario) =>
    Alert.alert("Eliminar comentario", "¿Seguro que quieres borrarlo?", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: async () => {
          try {
            await publicacionesApi.eliminarComentario(c.uuid);
            comentarios.updateItems((items) => items.filter((x) => x.uuid !== c.uuid));
            actualizar((p) => ({ ...p, numComentarios: Math.max(0, (p.numComentarios ?? 1) - 1) }));
          } catch (cause) {
            Alert.alert("No se ha podido eliminar", describeError(cause).message);
          }
        },
      },
    ]);

  const eliminarPublicacion = () =>
    Alert.alert("Eliminar publicación", "Se borrarán también los comentarios y los me gusta.", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: async () => {
          try {
            await publicacionesApi.eliminar(uuid);
            volver();
          } catch (cause) {
            Alert.alert("No se ha podido eliminar", describeError(cause).message);
          }
        },
      },
    ]);

  const header = (
    <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.spacing.sm }}>
      <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} />
      <Text variant="title3">Publicación</Text>
      {publicacion && user?.uuid === publicacion.autor.uuid ? (
        <IconButton name="trash-2" accessibilityLabel="Eliminar publicación" onPress={eliminarPublicacion} />
      ) : publicacion ? (
        <IconButton
          name="flag"
          accessibilityLabel="Denunciar publicación"
          onPress={() => abrirDenuncia(router, "publicacion", publicacion.uuid, { uuid: publicacion.autor.uuid, nombre: publicacion.autor.nombreCompleto })}
        />
      ) : (
        <View style={{ width: 42 }} />
      )}
    </View>
  );

  if (!publicacion) {
    const noExiste = carga.error instanceof ApiError && carga.error.status === 404;
    return (
      <Screen>
        {header}
        {carga.loading ? <LoadingView /> : noExiste ? <EmptyState icon="file-text" title="Publicación no encontrada" message="Puede que su autor la haya eliminado." /> : <ErrorState onRetry={carga.reload} />}
      </Screen>
    );
  }

  return (
    <Screen>
      {header}
      <View style={{ flex: 1 }}>
        <FlatList
          data={comentarios.items}
          keyExtractor={(c) => c.uuid}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.sm, gap: theme.spacing.md }}
          keyboardShouldPersistTaps="handled"
          onEndReached={comentarios.loadMore}
          onEndReachedThreshold={0.5}
          ListHeaderComponent={
            <View style={{ gap: theme.spacing.lg, marginBottom: theme.spacing.xs }}>
              <PublicacionCard publicacion={publicacion} onLike={() => alternarLike(publicacion)} enDetalle />
              <Text variant="title2">Comentarios</Text>
            </View>
          }
          renderItem={({ item }) => {
            return (
              <Pressable onLongPress={() => opcionesComentario(item)} delayLongPress={350} style={{ flexDirection: "row", gap: theme.spacing.md }}>
                <Pressable onPress={() => router.push({ pathname: "/usuario/[uuid]", params: { uuid: item.autor.uuid } })}>
                  <Avatar nombre={item.autor.nombreCompleto} fotoUrl={item.autor.fotoPerfilUrl} size={34} />
                </Pressable>
                <View style={{ flex: 1, backgroundColor: theme.colors.surface, borderRadius: theme.radius.lg, borderTopLeftRadius: theme.radius.sm, padding: theme.spacing.md, gap: 2 }}>
                  <View style={{ flexDirection: "row", justifyContent: "space-between", gap: theme.spacing.sm }}>
                    <Text variant="captionMedium" numberOfLines={1} style={{ flex: 1 }}>
                      {item.autor.nombreCompleto}
                    </Text>
                    <Text variant="caption" color="inkFaint">
                      {formatRelativo(item.fecha)}
                    </Text>
                  </View>
                  <Text variant="body">{item.contenido}</Text>
                </View>
              </Pressable>
            );
          }}
          ListEmptyComponent={
            comentarios.loading ? (
              <ActivityIndicator color={theme.colors.accent} />
            ) : (
              <Text variant="caption" color="inkMuted">
                Sé el primero en comentar.
              </Text>
            )
          }
          ListFooterComponent={<ListFooter loadingMore={comentarios.loadingMore} error={comentarios.items.length > 0 ? comentarios.error : null} />}
        />

        <View
          style={{
            flexDirection: "row",
            alignItems: "flex-end",
            gap: theme.spacing.sm,
            paddingHorizontal: theme.screenPadding,
            paddingTop: theme.spacing.sm,
            paddingBottom: (teclado > 0 ? teclado + insets.bottom : insets.bottom) + theme.spacing.sm,
            borderTopWidth: 1,
            borderTopColor: theme.colors.border,
            backgroundColor: theme.colors.surface,
          }}
        >
          <TextInput
            value={texto}
            onChangeText={setTexto}
            placeholder="Escribe un comentario…"
            placeholderTextColor={theme.colors.inkFaint}
            selectionColor={theme.colors.accent}
            multiline
            maxLength={280}
            style={[
              theme.typography.body,
              {
                flex: 1,
                maxHeight: 110,
                minHeight: 44,
                color: theme.colors.ink,
                backgroundColor: theme.colors.surfaceSunken,
                borderRadius: theme.radius.xl,
                paddingHorizontal: theme.spacing.lg,
                paddingTop: 11,
                paddingBottom: 11,
              },
            ]}
          />
          <IconButton name="send" variant="accent" size={18} accessibilityLabel="Enviar comentario" disabled={enviando || texto.trim().length === 0} onPress={comentar} />
        </View>
      </View>
    </Screen>
  );
}
