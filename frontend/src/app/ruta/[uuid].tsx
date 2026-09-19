import * as rutasApi from "@/api/rutas";
import { useAuth } from "@/auth/AuthContext";
import { DificultadBadge } from "@/components/DificultadBadge";
import { RutaMap } from "@/components/RutaMap";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Stars } from "@/components/ui/Stars";
import { Text } from "@/components/ui/Text";
import { ApiError } from "@/api/client";
import { describeError } from "@/lib/errors";
import { etiquetaTerreno, formatDuracion, formatKm, formatRelativo } from "@/lib/format";
import { useAsync } from "@/lib/useAsync";
import { abrirDenuncia } from "@/lib/denuncia";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import type { PuntoRuta, RutaResponse } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, ScrollView, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** Recorrido a dibujar: los puntos guardados o, si no hay, salida y llegada. */
function trackDe(ruta: RutaResponse): PuntoRuta[] {
  if (ruta.puntos && ruta.puntos.length > 0) return [...ruta.puntos].sort((a, b) => a.orden - b.orden);
  const extremos: PuntoRuta[] = [];
  if (ruta.latitudInicio != null && ruta.longitudInicio != null) extremos.push({ orden: 0, latitud: ruta.latitudInicio, longitud: ruta.longitudInicio });
  if (ruta.latitudFin != null && ruta.longitudFin != null) extremos.push({ orden: 1, latitud: ruta.latitudFin, longitud: ruta.longitudFin });
  return extremos;
}

export default function RutaDetalleScreen() {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { user } = useAuth();

  const ruta = useAsync(() => rutasApi.obtener(uuid), [uuid]);
  const valoraciones = useAsync(() => rutasApi.valoraciones(uuid, 0, 10), [uuid]);
  const [eliminando, setEliminando] = useState(false);

  // Al volver de valorar se actualizan la media y la lista.
  useRefocus(() => {
    ruta.reload();
    valoraciones.reload();
  });

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/rutas"));

  if (ruta.loading && !ruta.data) {
    return (
      <Screen>
        <LoadingView />
      </Screen>
    );
  }

  if (!ruta.data) {
    const noExiste = ruta.error instanceof ApiError && ruta.error.status === 404;
    return (
      <Screen>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} style={{ marginLeft: theme.spacing.sm }} />
        {noExiste ? <EmptyState icon="map" title="Ruta no encontrada" message="Puede que su creador la haya eliminado." /> : <ErrorState onRetry={ruta.reload} />}
      </Screen>
    );
  }

  const r = ruta.data;
  const esCreador = user?.uuid === r.creador.uuid;
  const track = trackDe(r);
  const miValoracion = valoraciones.data?.content.find((v) => v.autor.uuid === user?.uuid);

  const confirmarEliminar = () =>
    Alert.alert("Eliminar ruta", "Se borrarán también sus valoraciones. Esta acción no se puede deshacer.", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: async () => {
          setEliminando(true);
          try {
            await rutasApi.eliminar(r.uuid);
            volver();
          } catch (cause) {
            setEliminando(false);
            Alert.alert("No se ha podido eliminar", describeError(cause).message);
          }
        },
      },
    ]);

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: insets.bottom + theme.spacing.huge }}>
        <View style={{ height: 320, backgroundColor: theme.colors.surfaceSunken }}>
          {track.length > 0 ? (
            <RutaMap puntos={track} trazado={r.trazado} />
          ) : (
            <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
              <Feather name="map" size={40} color={theme.colors.inkFaint} />
            </View>
          )}
          <View style={{ position: "absolute", top: insets.top + theme.spacing.sm, left: theme.spacing.md, right: theme.spacing.md, flexDirection: "row", justifyContent: "space-between" }}>
            <IconButton name="arrow-left" variant="floating" accessibilityLabel="Volver" onPress={volver} />
            {esCreador ? (
              <IconButton name="trash-2" variant="floating" accessibilityLabel="Eliminar ruta" disabled={eliminando} onPress={confirmarEliminar} />
            ) : (
              <IconButton name="flag" variant="floating" accessibilityLabel="Denunciar ruta" onPress={() => abrirDenuncia(router, "ruta", r.uuid)} />
            )}
          </View>
        </View>

        <View style={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.xl, gap: theme.spacing.xl }}>
          <View style={{ gap: theme.spacing.sm }}>
            <DificultadBadge dificultad={r.dificultad} />
            <Text variant="display">{r.nombre}</Text>
            <Text variant="body" color="inkMuted">
              {r.puntoInicio} → {r.puntoFin}
            </Text>
          </View>

          <View style={{ flexDirection: "row", backgroundColor: theme.colors.surface, borderRadius: theme.radius.lg, borderWidth: 1, borderColor: theme.colors.border, paddingVertical: theme.spacing.md }}>
            {[
              { label: "Distancia", value: formatKm(r.distanciaKm) },
              { label: "Duración", value: formatDuracion(r.duracionEstimadaMin) },
              { label: "Terreno", value: etiquetaTerreno(r.tipoTerreno) },
            ].map((s, i) => (
              <View key={s.label} style={{ flex: 1, alignItems: "center", gap: 2, borderLeftWidth: i === 0 ? 0 : 1, borderLeftColor: theme.colors.border }}>
                <Text variant="title3">{s.value}</Text>
                <Text variant="overline" color="inkFaint">
                  {s.label}
                </Text>
              </View>
            ))}
          </View>

          <Pressable
            onPress={() => router.push({ pathname: "/usuario/[uuid]", params: { uuid: r.creador.uuid } })}
            style={({ pressed }) => ({ flexDirection: "row", alignItems: "center", gap: theme.spacing.md, opacity: pressed ? 0.7 : 1 })}
          >
            <Avatar nombre={r.creador.nombreCompleto} fotoUrl={r.creador.fotoPerfilUrl} size={40} />
            <View style={{ flex: 1 }}>
              <Text variant="caption" color="inkFaint">
                Trazada por
              </Text>
              <Text variant="bodyMedium">{r.creador.nombreCompleto}</Text>
            </View>
            <Text variant="caption" color="inkFaint">
              {formatRelativo(r.fechaCreacion)}
            </Text>
          </Pressable>

          {r.descripcion ? <Text variant="body">{r.descripcion}</Text> : null}

          <View style={{ flexDirection: "row", gap: theme.spacing.md }}>
            <Button
              label={miValoracion ? "Editar valoración" : "Valorar"}
              icon={<Feather name="star" size={16} color="#FFFFFF" />}
              style={{ flex: 1 }}
              onPress={() =>
                router.push({
                  pathname: "/valorar/[uuid]",
                  params: { uuid: r.uuid, nombre: r.nombre, puntuacion: miValoracion ? String(miValoracion.puntuacion) : undefined, comentario: miValoracion?.comentario ?? undefined },
                })
              }
            />
            <Button
              label="Compartir"
              variant="secondary"
              icon={<Feather name="share-2" size={16} color={theme.colors.ink} />}
              style={{ flex: 1 }}
              onPress={() => router.push({ pathname: "/publicacion/nueva", params: { rutaUuid: r.uuid, rutaNombre: r.nombre } })}
            />
          </View>

          <Button
            label="Organizar quedada"
            variant="ghost"
            fullWidth
            icon={<Feather name="calendar" size={16} color={theme.colors.accent} />}
            onPress={() => router.push({ pathname: "/quedada/nueva", params: { rutaUuid: r.uuid, rutaNombre: r.nombre } })}
            style={{ marginTop: -theme.spacing.md }}
          />

          <View style={{ gap: theme.spacing.md }}>
            <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
              <Text variant="title2">Valoraciones</Text>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
                <Stars value={r.valoracionMedia} size={15} />
                <Text variant="captionMedium" color="inkMuted">
                  {r.valoracionMedia != null ? r.valoracionMedia.toLocaleString("es-ES", { maximumFractionDigits: 1 }) : "—"} ({r.numValoraciones ?? 0})
                </Text>
              </View>
            </View>

            {valoraciones.data && valoraciones.data.content.length > 0 ? (
              valoraciones.data.content.map((v) => (
                <View key={v.uuid} style={{ backgroundColor: theme.colors.surface, borderRadius: theme.radius.lg, borderWidth: 1, borderColor: theme.colors.border, padding: theme.spacing.lg, gap: theme.spacing.sm }}>
                  <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.md }}>
                    <Avatar nombre={v.autor.nombreCompleto} fotoUrl={v.autor.fotoPerfilUrl} size={32} />
                    <View style={{ flex: 1 }}>
                      <Text variant="captionMedium">{v.autor.nombreCompleto}</Text>
                      <Text variant="caption" color="inkFaint">
                        {formatRelativo(v.fecha)}
                      </Text>
                    </View>
                    <Stars value={v.puntuacion} />
                  </View>
                  {v.comentario ? <Text variant="body">{v.comentario}</Text> : null}
                </View>
              ))
            ) : (
              <Text variant="caption" color="inkMuted">
                {valoraciones.loading ? "Cargando…" : "Nadie la ha valorado todavía. ¿La has rodado?"}
              </Text>
            )}
          </View>
        </View>
      </ScrollView>
    </View>
  );
}
