import { etiquetaTerreno, formatDuracion, formatKm } from "@/lib/format";
import { distanciaKm, type Coordenadas } from "@/lib/ubicacion";
import type { RutaSummary } from "@/types/dto";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { Pressable, View } from "react-native";
import { DificultadBadge } from "./DificultadBadge";
import { Stars } from "./ui/Stars";
import { Text } from "./ui/Text";

interface RutaCardProps {
  ruta: RutaSummary;
  /** Si se sabe dónde está el usuario, la tarjeta dice a cuánto le queda la salida. */
  desde?: Coordenadas | null;
}

export function RutaCard({ ruta, desde }: RutaCardProps) {
  const theme = useTheme();
  const router = useRouter();

  const aLaSalida =
    desde && ruta.latitudInicio != null && ruta.longitudInicio != null
      ? distanciaKm(desde, { latitud: ruta.latitudInicio, longitud: ruta.longitudInicio })
      : null;

  return (
    <Pressable
      onPress={() => router.push({ pathname: "/ruta/[uuid]", params: { uuid: ruta.uuid } })}
      style={({ pressed }) => ({
        backgroundColor: theme.colors.surface,
        borderRadius: theme.radius.lg,
        borderWidth: 1,
        borderColor: theme.colors.border,
        padding: theme.spacing.lg,
        gap: theme.spacing.md,
        opacity: pressed ? 0.85 : 1,
      })}
    >
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", gap: theme.spacing.md }}>
        <View style={{ flex: 1, gap: 2 }}>
          <Text variant="title2" numberOfLines={2}>
            {ruta.nombre}
          </Text>
          <Text variant="caption" color="inkMuted" numberOfLines={1}>
            {ruta.puntoInicio} → {ruta.puntoFin}
          </Text>
        </View>
        <Feather name="chevron-right" size={20} color={theme.colors.inkFaint} />
      </View>

      <View style={{ flexDirection: "row", gap: theme.spacing.xl }}>
        <Stat icon="navigation" value={formatKm(ruta.distanciaKm)} />
        <Stat icon="clock" value={formatDuracion(ruta.duracionEstimadaMin)} />
        <Stat icon="layers" value={etiquetaTerreno(ruta.tipoTerreno)} />
      </View>

      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.md }}>
          <DificultadBadge dificultad={ruta.dificultad} />
          {aLaSalida != null ? (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
              <Feather name="map-pin" size={13} color={theme.colors.inkMuted} />
              <Text variant="caption" color="inkMuted">
                a {aLaSalida < 10 ? aLaSalida.toFixed(1) : Math.round(aLaSalida)} km
              </Text>
            </View>
          ) : null}
        </View>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
          <Stars value={ruta.valoracionMedia} />
          <Text variant="caption" color="inkFaint">
            ({ruta.numValoraciones ?? 0})
          </Text>
        </View>
      </View>
    </Pressable>
  );
}

function Stat({ icon, value }: { icon: React.ComponentProps<typeof Feather>["name"]; value: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
      <Feather name={icon} size={14} color={theme.colors.accent} />
      <Text variant="captionMedium">{value}</Text>
    </View>
  );
}
