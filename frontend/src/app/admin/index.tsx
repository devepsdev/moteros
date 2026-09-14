import * as adminApi from "@/api/admin";
import { RutaCard } from "@/components/RutaCard";
import { Button } from "@/components/ui/Button";
import { ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { DIFICULTADES, etiquetaTerreno, etiquetaTipoMoto } from "@/lib/format";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import type { TipoMoto, TipoTerreno } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { RefreshControl, ScrollView, View } from "react-native";

const ESTADOS_QUEDADA: Record<string, string> = { programada: "Programadas", cancelada: "Canceladas", finalizada: "Finalizadas" };

/** Panel global de la plataforma. La ruta solo existe para administradores (Stack.Protected). */
export default function AdminScreen() {
  const theme = useTheme();
  const router = useRouter();
  const stats = useAsync(() => adminApi.estadisticas(), []);

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/perfil"));

  if (!stats.data) {
    return (
      <Screen>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} style={{ marginLeft: theme.spacing.sm }} />
        {stats.loading ? <LoadingView /> : <ErrorState onRetry={stats.reload} />}
      </Screen>
    );
  }

  const s = stats.data;
  const cifras: { icon: React.ComponentProps<typeof Feather>["name"]; label: string; valor: number; detalle?: string }[] = [
    { icon: "users", label: "Usuarios", valor: s.usuariosTotales, detalle: `${s.usuariosActivos} activos · ${s.administradores} admin` },
    { icon: "map", label: "Rutas", valor: s.rutas, detalle: `${s.valoraciones} valoraciones` },
    { icon: "calendar", label: "Quedadas", valor: s.quedadas, detalle: `${s.quedadasProximas} próximas` },
    { icon: "file-text", label: "Publicaciones", valor: s.publicaciones, detalle: `${s.comentarios} comentarios` },
    { icon: "tool", label: "Motos", valor: s.motos },
    { icon: "user-check", label: "Amistades", valor: s.amistadesAceptadas },
  ];

  // Dificultades en su orden natural, no en el que llegue del mapa.
  const porDificultad = DIFICULTADES.map((d) => ({ label: d.label, valor: s.rutasPorDificultad[d.value] ?? 0 }));

  return (
    <Screen>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: theme.spacing.huge }}
        refreshControl={<RefreshControl refreshing={stats.loading} onRefresh={stats.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
      >
        <View style={{ paddingHorizontal: theme.spacing.sm }}>
          <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} />
        </View>

        <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.xl }}>
          <View style={{ gap: theme.spacing.xs }}>
            <Text variant="overline" color="accent">
              Administración
            </Text>
            <Text variant="display">Plataforma</Text>
          </View>

          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: theme.spacing.md }}>
            {cifras.map((c) => (
              <View
                key={c.label}
                style={{
                  flexGrow: 1,
                  flexBasis: "45%",
                  padding: theme.spacing.lg,
                  gap: 2,
                  borderRadius: theme.radius.lg,
                  borderWidth: 1,
                  borderColor: theme.colors.border,
                  backgroundColor: theme.colors.surface,
                }}
              >
                <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
                  <Feather name={c.icon} size={14} color={theme.colors.accent} />
                  <Text variant="overline" color="inkFaint">
                    {c.label}
                  </Text>
                </View>
                <Text variant="stat">{c.valor.toLocaleString("es-ES")}</Text>
                {c.detalle ? (
                  <Text variant="caption" color="inkMuted" numberOfLines={1}>
                    {c.detalle}
                  </Text>
                ) : null}
              </View>
            ))}
          </View>

          <Button label="Gestionar usuarios" variant="secondary" fullWidth icon={<Feather name="users" size={16} color={theme.colors.ink} />} onPress={() => router.push("/admin/usuarios")} />

          <Altas altas={s.altasUsuariosPorMes} />

          <Barras titulo="Rutas por dificultad" datos={porDificultad} />
          <Barras titulo="Rutas por terreno" datos={Object.entries(s.rutasPorTerreno).map(([k, v]) => ({ label: etiquetaTerreno(k as TipoTerreno), valor: v }))} />
          <Barras titulo="Motos por tipo" datos={Object.entries(s.motosPorTipo).map(([k, v]) => ({ label: etiquetaTipoMoto(k as TipoMoto), valor: v }))} />
          <Barras titulo="Quedadas por estado" datos={Object.entries(s.quedadasPorEstado).map(([k, v]) => ({ label: ESTADOS_QUEDADA[k] ?? k, valor: v }))} />

          <View style={{ gap: theme.spacing.md }}>
            <Text variant="title2">Rutas mejor valoradas</Text>
            {s.topRutasPorValoracion.length === 0 ? (
              <Text variant="caption" color="inkMuted">
                Aún no hay valoraciones.
              </Text>
            ) : (
              s.topRutasPorValoracion.map((r) => <RutaCard key={r.uuid} ruta={r} />)
            )}
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}

/** Barras horizontales sencillas, en el orden en que llegan los datos. */
function Barras({ titulo, datos }: { titulo: string; datos: { label: string; valor: number }[] }) {
  const theme = useTheme();
  const max = Math.max(1, ...datos.map((d) => d.valor));
  return (
    <View style={{ gap: theme.spacing.md }}>
      <Text variant="title2">{titulo}</Text>
      {datos.length === 0 ? (
        <Text variant="caption" color="inkMuted">
          Sin datos todavía.
        </Text>
      ) : (
        datos.map((d) => (
          <View key={d.label} style={{ gap: 4 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <Text variant="caption" color="inkMuted">
                {d.label}
              </Text>
              <Text variant="captionMedium">{d.valor.toLocaleString("es-ES")}</Text>
            </View>
            <View style={{ height: 8, borderRadius: 4, backgroundColor: theme.colors.surfaceSunken, overflow: "hidden" }}>
              <View style={{ width: `${(d.valor / max) * 100}%`, height: "100%", borderRadius: 4, backgroundColor: theme.colors.accent }} />
            </View>
          </View>
        ))
      )}
    </View>
  );
}

/** Altas de usuarios por mes como columnas. */
function Altas({ altas }: { altas: { mes: string; total: number }[] }) {
  const theme = useTheme();
  const max = Math.max(1, ...altas.map((a) => a.total));
  const etiquetaMes = (mes: string) => {
    const [anio, m] = mes.split("-").map(Number);
    return new Date(anio, m - 1, 1).toLocaleDateString("es-ES", { month: "short" }).replace(".", "");
  };

  return (
    <View style={{ gap: theme.spacing.md }}>
      <Text variant="title2">Altas por mes</Text>
      {altas.length === 0 ? (
        <Text variant="caption" color="inkMuted">
          Sin altas todavía.
        </Text>
      ) : (
        <View style={{ flexDirection: "row", alignItems: "flex-end", gap: theme.spacing.sm, height: 140 }}>
          {altas.map((a) => (
            <View key={a.mes} style={{ flex: 1, alignItems: "center", gap: 4 }}>
              <Text variant="caption" color="inkMuted" style={{ fontSize: 11 }}>
                {a.total}
              </Text>
              <View style={{ width: "70%", height: Math.max(4, (a.total / max) * 96), borderRadius: 4, backgroundColor: theme.colors.accent }} />
              <Text variant="caption" color="inkFaint" style={{ fontSize: 11 }}>
                {etiquetaMes(a.mes)}
              </Text>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}
