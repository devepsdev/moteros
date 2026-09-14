import { ApiError } from "@/api/client";
import * as quedadasApi from "@/api/quedadas";
import { useAuth } from "@/auth/AuthContext";
import { EstadoBadge } from "@/components/QuedadaCard";
import { RutaCard } from "@/components/RutaCard";
import { RutaMap } from "@/components/RutaMap";
import { UsuarioFila } from "@/components/UsuarioFila";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { etiquetaNivel, fechaDeApi, formatDia, formatHora } from "@/lib/format";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import type { InscripcionQuedada } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert, ScrollView, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function QuedadaDetalleScreen() {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { user } = useAuth();

  const quedada = useAsync(() => quedadasApi.obtener(uuid), [uuid]);
  const [ocupado, setOcupado] = useState(false);
  // Hora de referencia fijada al abrir la pantalla (el render ha de ser puro).
  const [ahora] = useState(() => Date.now());

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/quedadas"));

  if (!quedada.data) {
    const noExiste = quedada.error instanceof ApiError && quedada.error.status === 404;
    return (
      <Screen>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} style={{ marginLeft: theme.spacing.sm }} />
        {quedada.loading ? (
          <LoadingView />
        ) : noExiste ? (
          <EmptyState icon="calendar" title="Quedada no encontrada" message="Puede que su organizador la haya eliminado." />
        ) : (
          <ErrorState onRetry={quedada.reload} />
        )}
      </Screen>
    );
  }

  const q = quedada.data;
  const fecha = fechaDeApi(q.fechaHora);
  const esOrganizador = user?.uuid === q.organizador.uuid;
  const programada = q.estado === "programada";
  const yaPasada = fecha.getTime() < ahora;
  const apuntado = q.inscripcionUsuarioActual === "confirmado" || q.inscripcionUsuarioActual === "pendiente";
  const completa = q.plazasLibres === 0;
  const inscritos = (q.inscritos ?? []).filter((i) => i.estado !== "cancelado");

  /** Ejecuta una acción que modifica la quedada y recarga el detalle. */
  const accion = async (fn: () => Promise<unknown>, titulo: string, despues?: () => void) => {
    setOcupado(true);
    try {
      await fn();
      if (despues) despues();
      else quedada.reload();
    } catch (cause) {
      Alert.alert(titulo, describeError(cause).message);
    } finally {
      setOcupado(false);
    }
  };

  const confirmar = (titulo: string, mensaje: string, boton: string, fn: () => void) =>
    Alert.alert(titulo, mensaje, [
      { text: "Volver", style: "cancel" },
      { text: boton, style: "destructive", onPress: fn },
    ]);

  const quitarInscrito = (i: InscripcionQuedada) =>
    confirmar("Quitar participante", `¿Quitar a ${i.usuario.nombreCompleto} de la quedada?`, "Quitar", () =>
      accion(() => quedadasApi.cambiarEstadoInscripcion(q.uuid, i.usuario.uuid, "cancelado"), "No se ha podido quitar")
    );

  const datos: { icon: React.ComponentProps<typeof Feather>["name"]; titulo: string; valor: string }[] = [
    { icon: "calendar", titulo: "Cuándo", valor: `${formatDia(fecha)} · ${formatHora(fecha)}` },
    { icon: "map-pin", titulo: "Punto de encuentro", valor: q.puntoEncuentro },
    { icon: "bar-chart-2", titulo: "Nivel", valor: etiquetaNivel(q.nivelRecomendado) },
    {
      icon: "users",
      titulo: "Participantes",
      valor: q.maxParticipantes ? `${q.numInscritos ?? 0} de ${q.maxParticipantes} · ${q.plazasLibres ?? 0} libres` : `${q.numInscritos ?? 0} apuntados · sin límite`,
    },
  ];

  const hayMapa = q.latitudEncuentro != null && q.longitudEncuentro != null;

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: insets.bottom + theme.spacing.huge }}>
        {hayMapa ? (
          <View style={{ height: 240, backgroundColor: theme.colors.surfaceSunken }}>
            <RutaMap puntos={[{ orden: 0, latitud: q.latitudEncuentro!, longitud: q.longitudEncuentro! }]} etiquetaInicio="Punto de encuentro" />
            <View style={{ position: "absolute", top: insets.top + theme.spacing.sm, left: theme.spacing.md }}>
              <IconButton name="arrow-left" variant="floating" accessibilityLabel="Volver" onPress={volver} />
            </View>
          </View>
        ) : (
          <View style={{ paddingTop: insets.top, paddingHorizontal: theme.spacing.sm }}>
            <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} />
          </View>
        )}

        <View style={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.xl, gap: theme.spacing.xl }}>
          <View style={{ gap: theme.spacing.sm }}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
              <Text variant="overline" color="accent">
                Quedada
              </Text>
              {!programada ? <EstadoBadge estado={q.estado} /> : null}
            </View>
            <Text variant="display">{q.titulo}</Text>
          </View>

          <View style={{ backgroundColor: theme.colors.surface, borderRadius: theme.radius.lg, borderWidth: 1, borderColor: theme.colors.border }}>
            {datos.map((d, i) => (
              <View key={d.titulo} style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.md, padding: theme.spacing.lg, borderTopWidth: i === 0 ? 0 : 1, borderTopColor: theme.colors.border }}>
                <Feather name={d.icon} size={18} color={theme.colors.accent} />
                <View style={{ flex: 1 }}>
                  <Text variant="caption" color="inkFaint">
                    {d.titulo}
                  </Text>
                  <Text variant="bodyMedium">{d.valor}</Text>
                </View>
              </View>
            ))}
          </View>

          {/* Acciones */}
          {esOrganizador ? (
            <View style={{ gap: theme.spacing.sm }}>
              {programada && yaPasada ? (
                <Button label="Marcar como finalizada" fullWidth loading={ocupado} onPress={() => accion(() => quedadasApi.cambiarEstado(q.uuid, "finalizada"), "No se ha podido actualizar")} />
              ) : null}
              {programada ? (
                <Button
                  label="Cancelar quedada"
                  variant="secondary"
                  fullWidth
                  disabled={ocupado}
                  onPress={() =>
                    confirmar("Cancelar quedada", "Se avisará a todos los apuntados.", "Cancelar quedada", () =>
                      accion(() => quedadasApi.cambiarEstado(q.uuid, "cancelada"), "No se ha podido cancelar")
                    )
                  }
                />
              ) : null}
              <Button
                label="Eliminar quedada"
                variant="ghost"
                fullWidth
                disabled={ocupado}
                onPress={() =>
                  confirmar("Eliminar quedada", "Se borrará junto con sus inscripciones. No se puede deshacer.", "Eliminar", () =>
                    accion(() => quedadasApi.eliminar(q.uuid), "No se ha podido eliminar", volver)
                  )
                }
              />
            </View>
          ) : programada && !yaPasada ? (
            apuntado ? (
              <View style={{ gap: theme.spacing.sm }}>
                <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm, padding: theme.spacing.md, borderRadius: theme.radius.md, backgroundColor: theme.colors.supportSoft }}>
                  <Feather name="check-circle" size={18} color={theme.colors.support} />
                  <Text variant="bodyMedium" color="support">
                    Estás apuntado
                  </Text>
                </View>
                <Button
                  label="Borrarme"
                  variant="ghost"
                  fullWidth
                  disabled={ocupado}
                  onPress={() => confirmar("Borrarte de la quedada", "Dejarás libre tu plaza.", "Borrarme", () => accion(() => quedadasApi.cancelarInscripcion(q.uuid), "No se ha podido cancelar"))}
                />
              </View>
            ) : (
              <Button
                label={completa ? "Quedada completa" : "Apuntarme"}
                size="lg"
                fullWidth
                disabled={completa}
                loading={ocupado}
                icon={completa ? undefined : <Feather name="plus" size={18} color="#FFFFFF" />}
                onPress={() => accion(() => quedadasApi.inscribirse(q.uuid), "No te has podido apuntar")}
              />
            )
          ) : null}

          {q.descripcion ? (
            <View style={{ gap: theme.spacing.sm }}>
              <Text variant="title2">Detalles</Text>
              <Text variant="body">{q.descripcion}</Text>
            </View>
          ) : null}

          {q.ruta ? (
            <View style={{ gap: theme.spacing.md }}>
              <Text variant="title2">Ruta</Text>
              <RutaCard ruta={q.ruta} />
            </View>
          ) : null}

          <View style={{ gap: theme.spacing.xs }}>
            <Text variant="title2">Organiza</Text>
            <UsuarioFila usuario={q.organizador} />
          </View>

          <View style={{ gap: theme.spacing.xs }}>
            <Text variant="title2">Apuntados ({inscritos.length})</Text>
            {inscritos.length === 0 ? (
              <Text variant="caption" color="inkMuted">
                Todavía no se ha apuntado nadie.
              </Text>
            ) : (
              inscritos.map((i) => (
                <UsuarioFila key={i.uuid} usuario={i.usuario} detalle={i.estado === "pendiente" ? "Pendiente de confirmar" : `@${i.usuario.nombreUsuario}`}>
                  {esOrganizador && programada ? <IconButton name="user-x" size={18} color={theme.colors.inkMuted} accessibilityLabel={`Quitar a ${i.usuario.nombreCompleto}`} onPress={() => quitarInscrito(i)} /> : null}
                </UsuarioFila>
              ))
            )}
          </View>
        </View>
      </ScrollView>
    </View>
  );
}
