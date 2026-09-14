import * as motosApi from "@/api/motos";
import * as rutasApi from "@/api/rutas";
import { MotoCard } from "@/components/MotoCard";
import { RutaCard } from "@/components/RutaCard";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Text } from "@/components/ui/Text";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import type { UsuarioResponse } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { Pressable, ScrollView, View } from "react-native";

interface PerfilViewProps {
  usuario: UsuarioResponse;
  esPropio: boolean;
  /** Cambia para forzar la recarga de motos y rutas (p. ej. al volver a la pantalla). */
  reloadKey?: number;
  /** Acciones bajo la cabecera en perfiles ajenos (amistad, mensaje). */
  acciones?: React.ReactNode;
}

/** Cabecera, motos y rutas de un usuario. Compartido por "Perfil" y el perfil ajeno. */
export function PerfilView({ usuario, esPropio, reloadKey = 0, acciones }: PerfilViewProps) {
  const theme = useTheme();
  const router = useRouter();

  const motos = useAsync(() => (esPropio ? motosApi.mias() : motosApi.deUsuario(usuario.uuid)), [usuario.uuid, esPropio, reloadKey]);
  const rutas = useAsync(() => rutasApi.deUsuario(usuario.uuid, 0, 5), [usuario.uuid, reloadKey]);

  const stats = [
    { label: "Motos", value: usuario.numMotos ?? motos.data?.length ?? 0 },
    { label: "Rutas", value: usuario.numRutas ?? rutas.data?.pageable.totalElements ?? 0 },
    { label: "Amigos", value: usuario.numAmigos ?? 0 },
  ];

  return (
    <View style={{ gap: theme.spacing.xxl }}>
      <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.lg }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.lg }}>
          <Avatar nombre={usuario.nombreCompleto} fotoUrl={usuario.fotoPerfilUrl} size={76} />
          <View style={{ flex: 1, gap: 2 }}>
            <Text variant="title1" numberOfLines={2}>
              {usuario.nombreCompleto}
            </Text>
            <Text variant="caption" color="inkMuted">
              @{usuario.nombreUsuario}
            </Text>
            {usuario.ciudad ? (
              <View style={{ flexDirection: "row", alignItems: "center", gap: 4, marginTop: 2 }}>
                <Feather name="map-pin" size={12} color={theme.colors.inkFaint} />
                <Text variant="caption" color="inkFaint">
                  {usuario.ciudad}
                </Text>
              </View>
            ) : null}
          </View>
        </View>

        {usuario.biografia ? <Text variant="body">{usuario.biografia}</Text> : null}

        <View
          style={{
            flexDirection: "row",
            backgroundColor: theme.colors.surface,
            borderRadius: theme.radius.lg,
            borderWidth: 1,
            borderColor: theme.colors.border,
            paddingVertical: theme.spacing.md,
          }}
        >
          {stats.map((s, i) => (
            <Pressable
              key={s.label}
              disabled={s.label !== "Amigos"}
              onPress={() =>
                router.push(esPropio ? "/amigos" : { pathname: "/amigos", params: { usuarioUuid: usuario.uuid, nombre: usuario.nombreCompleto } })
              }
              style={({ pressed }) => ({ flex: 1, alignItems: "center", borderLeftWidth: i === 0 ? 0 : 1, borderLeftColor: theme.colors.border, opacity: pressed ? 0.6 : 1 })}
            >
              <Text variant="stat">{s.value}</Text>
              <Text variant="overline" color={s.label === "Amigos" ? "accent" : "inkFaint"}>
                {s.label}
              </Text>
            </Pressable>
          ))}
        </View>

        {esPropio ? (
          <Button label="Editar perfil" variant="secondary" fullWidth icon={<Feather name="edit-2" size={15} color={theme.colors.ink} />} onPress={() => router.push("/cuenta/datos")} />
        ) : (
          acciones
        )}
      </View>

      <Seccion titulo="Garaje" accion={esPropio ? { label: "Añadir", onPress: () => router.push("/moto/editar") } : undefined}>
        {motos.data && motos.data.length > 0 ? (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.md }}>
            {motos.data.map((m) => (
              <MotoCard
                key={m.uuid}
                moto={m}
                onPress={esPropio ? () => router.push({ pathname: "/moto/editar", params: { uuid: m.uuid } }) : undefined}
              />
            ))}
          </ScrollView>
        ) : (
          <Vacio texto={motos.loading ? "Cargando…" : esPropio ? "Añade tu moto para que el resto sepa con qué ruedas." : "Sin motos todavía."} />
        )}
      </Seccion>

      <Seccion titulo="Rutas creadas">
        <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.md }}>
          {rutas.data && rutas.data.content.length > 0 ? (
            rutas.data.content.map((r) => <RutaCard key={r.uuid} ruta={r} />)
          ) : (
            <Vacio texto={rutas.loading ? "Cargando…" : esPropio ? "Todavía no has trazado ninguna ruta." : "Sin rutas todavía."} inset={false} />
          )}
        </View>
      </Seccion>
    </View>
  );
}

function Seccion({ titulo, accion, children }: { titulo: string; accion?: { label: string; onPress: () => void }; children: React.ReactNode }) {
  const theme = useTheme();
  return (
    <View style={{ gap: theme.spacing.md }}>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.screenPadding }}>
        <Text variant="title2">{titulo}</Text>
        {accion ? (
          <Pressable onPress={accion.onPress} hitSlop={8} style={({ pressed }) => ({ flexDirection: "row", alignItems: "center", gap: 4, opacity: pressed ? 0.6 : 1 })}>
            <Feather name="plus" size={15} color={theme.colors.accent} />
            <Text variant="captionMedium" color="accent">
              {accion.label}
            </Text>
          </Pressable>
        ) : null}
      </View>
      {children}
    </View>
  );
}

function Vacio({ texto, inset = true }: { texto: string; inset?: boolean }) {
  const theme = useTheme();
  return (
    <View
      style={{
        marginHorizontal: inset ? theme.screenPadding : 0,
        padding: theme.spacing.lg,
        borderRadius: theme.radius.lg,
        borderWidth: 1,
        borderStyle: "dashed",
        borderColor: theme.colors.borderStrong,
      }}
    >
      <Text variant="caption" color="inkMuted" center>
        {texto}
      </Text>
    </View>
  );
}
