import * as amistadesApi from "@/api/amistades";
import { useAuth } from "@/auth/AuthContext";
import { UsuarioFila } from "@/components/UsuarioFila";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Segmented } from "@/components/ui/Segmented";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { formatRelativo } from "@/lib/format";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import type { Amistad, PageResponse, UsuarioSummary } from "@/types/dto";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert, FlatList, RefreshControl, View } from "react-native";

type Vista = "amigos" | "recibidas" | "enviadas";

/** Fila común de las tres vistas: amigo directo o solicitud (con el otro usuario ya resuelto). */
type Item = { key: string; usuario: UsuarioSummary; amistad?: Amistad };

const aItems = <T,>(page: PageResponse<T>, map: (x: T) => Item): PageResponse<Item> => ({ ...page, content: page.content.map(map) });

/** Amigos propios con solicitudes, o (con `?usuarioUuid=`) solo la lista de amigos de otro usuario. */
export default function AmigosScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user, refreshProfile } = useAuth();
  const params = useLocalSearchParams<{ usuarioUuid?: string; nombre?: string }>();
  const ajeno = Boolean(params.usuarioUuid && params.usuarioUuid !== user?.uuid);
  const usuarioUuid = ajeno ? params.usuarioUuid! : (user?.uuid ?? "");

  const [vista, setVista] = useState<Vista>("amigos");
  const [ocupado, setOcupado] = useState<string | null>(null);

  const lista = usePagedList<Item>(
    async (page) => {
      if (vista === "recibidas") return aItems(await amistadesApi.recibidas(page), (a) => ({ key: a.uuid, usuario: a.solicitante, amistad: a }));
      if (vista === "enviadas") return aItems(await amistadesApi.enviadas(page), (a) => ({ key: a.uuid, usuario: a.destinatario, amistad: a }));
      return aItems(await amistadesApi.amigos(usuarioUuid, page), (u) => ({ key: u.uuid, usuario: u }));
    },
    [vista, usuarioUuid]
  );

  const quitar = (key: string) => lista.updateItems((items) => items.filter((i) => i.key !== key));

  const ejecutar = async (item: Item, fn: () => Promise<unknown>) => {
    setOcupado(item.key);
    try {
      await fn();
      quitar(item.key);
      refreshProfile();
    } catch (cause) {
      Alert.alert("No se ha podido completar", describeError(cause).message);
    } finally {
      setOcupado(null);
    }
  };

  const vacios: Record<Vista, { title: string; message: string }> = {
    amigos: ajeno
      ? { title: "Sin amigos todavía", message: "Este motero aún no ha añadido a nadie." }
      : { title: "Todavía no tienes amigos", message: "Busca a otros moteros y envíales una solicitud." },
    recibidas: { title: "Sin solicitudes", message: "Cuando alguien quiera ser tu amigo lo verás aquí." },
    enviadas: { title: "Nada pendiente", message: "Las solicitudes que envíes aparecerán aquí hasta que respondan." },
  };

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.spacing.sm }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={() => (router.canGoBack() ? router.back() : router.replace("/perfil"))} />
        {!ajeno ? <IconButton name="user-plus" accessibilityLabel="Buscar moteros" onPress={() => router.push("/buscar")} /> : null}
      </View>

      <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.xs, marginBottom: theme.spacing.md }}>
        <Text variant="overline" color="accent">
          {ajeno ? (params.nombre ?? "Motero") : "Tu grupo"}
        </Text>
        <Text variant="display">Amigos</Text>
      </View>

      {!ajeno ? (
        <Segmented
          style={{ marginHorizontal: theme.screenPadding, marginBottom: theme.spacing.sm }}
          value={vista}
          onChange={setVista}
          options={[
            { value: "amigos", label: "Amigos" },
            { value: "recibidas", label: "Recibidas" },
            { value: "enviadas", label: "Enviadas" },
          ]}
        />
      ) : null}

      {lista.loading && lista.items.length === 0 ? (
        <LoadingView />
      ) : lista.error && lista.items.length === 0 ? (
        <ErrorState onRetry={lista.reload} />
      ) : (
        <FlatList
          data={lista.items}
          keyExtractor={(i) => i.key}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, flexGrow: 1 }}
          onEndReached={lista.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={lista.loading && lista.items.length > 0} onRefresh={lista.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          renderItem={({ item }) => {
            if (vista === "recibidas" && item.amistad) {
              const a = item.amistad;
              return (
                <UsuarioFila usuario={item.usuario} detalle={`Te lo pidió ${formatRelativo(a.fecha)}`}>
                  <IconButton name="x" variant="surface" size={16} accessibilityLabel="Rechazar" disabled={ocupado === item.key} onPress={() => ejecutar(item, () => amistadesApi.responder(a.uuid, false))} />
                  <IconButton name="check" variant="accent" size={16} accessibilityLabel="Aceptar" disabled={ocupado === item.key} onPress={() => ejecutar(item, () => amistadesApi.responder(a.uuid, true))} />
                </UsuarioFila>
              );
            }
            if (vista === "enviadas") {
              return (
                <UsuarioFila usuario={item.usuario} detalle={`Enviada ${item.amistad ? formatRelativo(item.amistad.fecha) : ""}`}>
                  <Button label="Retirar" variant="ghost" disabled={ocupado === item.key} onPress={() => ejecutar(item, () => amistadesApi.eliminar(item.usuario.uuid))} />
                </UsuarioFila>
              );
            }
            return (
              <UsuarioFila usuario={item.usuario}>
                {!ajeno ? (
                  <IconButton
                    name="message-circle"
                    variant="surface"
                    size={16}
                    accessibilityLabel={`Escribir a ${item.usuario.nombreCompleto}`}
                    onPress={() =>
                      router.push({
                        pathname: "/chat/usuario/[usuarioUuid]",
                        params: { usuarioUuid: item.usuario.uuid, nombre: item.usuario.nombreCompleto, foto: item.usuario.fotoPerfilUrl ?? undefined },
                      })
                    }
                  />
                ) : null}
              </UsuarioFila>
            );
          }}
          ListEmptyComponent={
            <EmptyState
              icon="users"
              title={vacios[vista].title}
              message={vacios[vista].message}
              actionLabel={!ajeno && vista !== "recibidas" ? "Buscar moteros" : undefined}
              onAction={() => router.push("/buscar")}
            />
          }
          ListFooterComponent={<ListFooter loadingMore={lista.loadingMore} error={lista.items.length > 0 ? lista.error : null} />}
        />
      )}
    </Screen>
  );
}
