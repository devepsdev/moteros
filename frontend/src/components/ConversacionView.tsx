import * as chatApi from "@/api/chat";
import { Avatar } from "@/components/ui/Avatar";
import { ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { confirmarBloqueo } from "@/lib/bloqueo";
import { abrirDenuncia } from "@/lib/denuncia";
import { describeError } from "@/lib/errors";
import { fechaDeApi, formatDia, formatHora } from "@/lib/format";
import { refrescarNoLeidos } from "@/lib/noLeidos";
import { usePagedList } from "@/lib/usePagedList";
import { useDesplazamientoTeclado } from "@/lib/useTeclado";
import { useTheme } from "@/theme";
import type { Mensaje, PageResponse } from "@/types/dto";
import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useState } from "react";
import { ActivityIndicator, Alert, FlatList, Pressable, TextInput, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const INTERVALO_MS = 5_000;

const PAGINA_VACIA: PageResponse<Mensaje> = {
  content: [],
  pageable: { page: 0, size: 0, sort: "", totalPages: 0, totalElements: 0, numberOfElements: 0, first: true, last: true, empty: true },
};

interface ConversacionViewProps {
  /** null si aún no hay conversación: se crea al enviar el primer mensaje. */
  conversacionUuid: string | null;
  interlocutor: { uuid: string; nombre: string; foto?: string | null };
}

/** Añade al principio (lista invertida: más reciente primero) los mensajes que aún no están. */
function fusionar(actuales: Mensaje[], nuevos: Mensaje[]): Mensaje[] {
  const vistos = new Set(actuales.map((m) => m.uuid));
  const anadir = nuevos.filter((m) => !vistos.has(m.uuid));
  return anadir.length ? [...anadir, ...actuales] : actuales;
}

export function ConversacionView({ conversacionUuid: uuidInicial, interlocutor }: ConversacionViewProps) {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  // El teclado no encoge la ventana en Android a pantalla completa: se sube el cuadro a mano.
  const teclado = useDesplazamientoTeclado();

  const [conversacionUuid, setConversacionUuid] = useState(uuidInicial);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);

  const mensajes = usePagedList(
    async (page) => {
      if (!conversacionUuid) return PAGINA_VACIA;
      const res = await chatApi.mensajes(conversacionUuid, page);
      // Pedir mensajes los marca como leídos: se actualiza la insignia de la pestaña.
      if (page === 0) refrescarNoLeidos();
      return res;
    },
    [conversacionUuid]
  );

  // Sin push: mientras la conversación está abierta se buscan mensajes nuevos cada pocos segundos.
  const { updateItems } = mensajes;
  useFocusEffect(
    useCallback(() => {
      if (!conversacionUuid) return;
      const id = setInterval(() => {
        chatApi
          .mensajes(conversacionUuid, 0)
          .then((res) => {
            updateItems((items) => fusionar(items, res.content));
            refrescarNoLeidos();
          })
          .catch(() => {});
      }, INTERVALO_MS);
      return () => clearInterval(id);
    }, [conversacionUuid, updateItems])
  );

  const enviar = async () => {
    const contenido = texto.trim();
    if (!contenido) return;
    setEnviando(true);
    try {
      const nuevo = conversacionUuid ? await chatApi.enviar(conversacionUuid, contenido) : await chatApi.enviarAUsuario(interlocutor.uuid, contenido);
      setTexto("");
      if (conversacionUuid) updateItems((items) => fusionar(items, [nuevo]));
      // Primer mensaje: al fijar el uuid la lista se recarga desde el servidor.
      else setConversacionUuid(nuevo.conversacionUuid);
    } catch (cause) {
      Alert.alert("No se ha podido enviar", describeError(cause).message);
    } finally {
      setEnviando(false);
    }
  };

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/chat"));

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm, paddingHorizontal: theme.spacing.sm, paddingBottom: theme.spacing.sm, borderBottomWidth: 1, borderBottomColor: theme.colors.border }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} />
        <Pressable
          onPress={() => router.push({ pathname: "/usuario/[uuid]", params: { uuid: interlocutor.uuid } })}
          style={({ pressed }) => ({ flex: 1, flexDirection: "row", alignItems: "center", gap: theme.spacing.md, opacity: pressed ? 0.7 : 1 })}
        >
          <Avatar nombre={interlocutor.nombre} fotoUrl={interlocutor.foto} size={36} />
          <Text variant="title3" numberOfLines={1} style={{ flex: 1 }}>
            {interlocutor.nombre}
          </Text>
        </Pressable>
        <IconButton
          name="more-vertical"
          accessibilityLabel="Más opciones"
          onPress={() => {
            const autor = { uuid: interlocutor.uuid, nombre: interlocutor.nombre };
            Alert.alert(interlocutor.nombre, "Para denunciar un mensaje concreto, mantenlo pulsado.", [
              { text: "Cancelar", style: "cancel" },
              { text: "Denunciar usuario", onPress: () => abrirDenuncia(router, "usuario", autor.uuid, autor) },
              {
                text: "Bloquear",
                style: "destructive",
                // Tras bloquear, la conversación deja de existir para los dos.
                onPress: () =>
                  confirmarBloqueo(autor, () => {
                    refrescarNoLeidos();
                    router.replace("/chat");
                  }),
              },
            ]);
          }}
        />
      </View>

      <View style={{ flex: 1 }}>
        {mensajes.loading && mensajes.items.length === 0 ? (
          <LoadingView />
        ) : mensajes.error && mensajes.items.length === 0 ? (
          <ErrorState onRetry={mensajes.reload} />
        ) : (
          <FlatList
            inverted
            data={mensajes.items}
            keyExtractor={(m) => m.uuid}
            contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md, flexGrow: 1 }}
            keyboardShouldPersistTaps="handled"
            onEndReached={mensajes.loadMore}
            onEndReachedThreshold={0.3}
            renderItem={({ item, index }) => {
              // Con la lista invertida, el "anterior" en el tiempo es el siguiente del array.
              const anterior = mensajes.items[index + 1];
              const fecha = fechaDeApi(item.fechaEnvio);
              const nuevoDia = !anterior || fechaDeApi(anterior.fechaEnvio).toDateString() !== fecha.toDateString();
              const agrupado = anterior && !nuevoDia && anterior.propio === item.propio;
              return (
                <View>
                  {nuevoDia ? (
                    <Text variant="overline" color="inkFaint" center style={{ marginVertical: theme.spacing.md }}>
                      {formatDia(fecha)}
                    </Text>
                  ) : null}
                  <Burbuja
                    mensaje={item}
                    agrupado={Boolean(agrupado)}
                    onDenunciar={() => abrirDenuncia(router, "mensaje", item.uuid, { uuid: interlocutor.uuid, nombre: interlocutor.nombre })}
                  />
                </View>
              );
            }}
            ListEmptyComponent={
              <View style={{ flex: 1, alignItems: "center", justifyContent: "center", transform: [{ scaleY: -1 }], gap: theme.spacing.sm }}>
                <Avatar nombre={interlocutor.nombre} fotoUrl={interlocutor.foto} size={64} />
                <Text variant="body" color="inkMuted" center>
                  Escribe a {interlocutor.nombre} para empezar la conversación.
                </Text>
              </View>
            }
            ListFooterComponent={mensajes.loadingMore ? <ActivityIndicator color={theme.colors.accent} style={{ marginVertical: theme.spacing.md }} /> : null}
          />
        )}

        <View
          style={{
            flexDirection: "row",
            alignItems: "flex-end",
            gap: theme.spacing.sm,
            paddingHorizontal: theme.screenPadding,
            paddingTop: theme.spacing.sm,
            paddingBottom: (teclado > 0 ? teclado : insets.bottom) + theme.spacing.sm,
            borderTopWidth: 1,
            borderTopColor: theme.colors.border,
            backgroundColor: theme.colors.surface,
          }}
        >
          <TextInput
            value={texto}
            onChangeText={setTexto}
            placeholder="Mensaje"
            placeholderTextColor={theme.colors.inkFaint}
            selectionColor={theme.colors.accent}
            multiline
            maxLength={5000}
            style={[
              theme.typography.body,
              {
                flex: 1,
                maxHeight: 120,
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
          <IconButton name="send" variant="accent" size={18} accessibilityLabel="Enviar mensaje" disabled={enviando || texto.trim().length === 0} onPress={enviar} />
        </View>
      </View>
    </Screen>
  );
}

function Burbuja({ mensaje: m, agrupado, onDenunciar }: { mensaje: Mensaje; agrupado: boolean; onDenunciar: () => void }) {
  const theme = useTheme();
  const propio = Boolean(m.propio);
  return (
    <View style={{ alignItems: propio ? "flex-end" : "flex-start", marginTop: agrupado ? 3 : theme.spacing.sm }}>
      <Pressable
        // Los mensajes recibidos se denuncian con una pulsación larga.
        onLongPress={
          propio
            ? undefined
            : () =>
                Alert.alert("Mensaje", undefined, [
                  { text: "Cancelar", style: "cancel" },
                  { text: "Denunciar mensaje", onPress: onDenunciar },
                ])
        }
        delayLongPress={350}
        style={{
          maxWidth: "80%",
          paddingHorizontal: theme.spacing.md,
          paddingTop: theme.spacing.sm,
          paddingBottom: 6,
          borderRadius: theme.radius.lg,
          borderBottomRightRadius: propio ? theme.radius.sm : theme.radius.lg,
          borderBottomLeftRadius: propio ? theme.radius.lg : theme.radius.sm,
          backgroundColor: propio ? theme.colors.accent : theme.colors.surface,
          borderWidth: propio ? 0 : 1,
          borderColor: theme.colors.border,
        }}
      >
        <Text variant="body" style={propio ? { color: "#FFFFFF" } : undefined}>
          {m.contenido}
        </Text>
        <Text variant="caption" style={{ alignSelf: "flex-end", fontSize: 11, marginTop: 2, color: propio ? "rgba(255,255,255,0.75)" : theme.colors.inkFaint }}>
          {formatHora(fechaDeApi(m.fechaEnvio))}
          {propio && m.leido ? " · Leído" : ""}
        </Text>
      </Pressable>
    </View>
  );
}
