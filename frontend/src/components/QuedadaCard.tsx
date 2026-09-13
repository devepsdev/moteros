import { etiquetaNivel, fechaDeApi, formatHora } from "@/lib/format";
import type { EstadoQuedada, QuedadaSummary } from "@/types/dto";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { Pressable, View } from "react-native";
import { Text } from "./ui/Text";

export function QuedadaCard({ quedada: q }: { quedada: QuedadaSummary }) {
  const theme = useTheme();
  const router = useRouter();
  const fecha = fechaDeApi(q.fechaHora);
  const inactiva = q.estado !== "programada";
  const plazas = q.maxParticipantes ? `${q.numInscritos ?? 0}/${q.maxParticipantes}` : `${q.numInscritos ?? 0}`;

  return (
    <Pressable
      onPress={() => router.push({ pathname: "/quedada/[uuid]", params: { uuid: q.uuid } })}
      style={({ pressed }) => ({
        flexDirection: "row",
        backgroundColor: theme.colors.surface,
        borderRadius: theme.radius.lg,
        borderWidth: 1,
        borderColor: theme.colors.border,
        overflow: "hidden",
        opacity: pressed ? 0.85 : inactiva ? 0.6 : 1,
      })}
    >
      {/* Taco de calendario */}
      <View style={{ width: 72, alignItems: "center", justifyContent: "center", backgroundColor: inactiva ? theme.colors.surfaceSunken : theme.colors.accentSoft, paddingVertical: theme.spacing.md }}>
        <Text variant="overline" color={inactiva ? "inkFaint" : "accent"}>
          {fecha.toLocaleDateString("es-ES", { month: "short" }).replace(".", "")}
        </Text>
        <Text variant="stat" color={inactiva ? "inkMuted" : "accent"} style={{ lineHeight: 34 }}>
          {fecha.getDate()}
        </Text>
        <Text variant="caption" color={inactiva ? "inkFaint" : "accent"}>
          {fecha.toLocaleDateString("es-ES", { weekday: "short" }).replace(".", "")}
        </Text>
      </View>

      <View style={{ flex: 1, padding: theme.spacing.md, gap: 4 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
          <Text variant="title3" numberOfLines={1} style={{ flex: 1 }}>
            {q.titulo}
          </Text>
          {inactiva ? <EstadoBadge estado={q.estado} /> : null}
        </View>
        <Linea icon="clock" texto={`${formatHora(fecha)} · ${q.puntoEncuentro}`} />
        {q.rutaNombre ? <Linea icon="map" texto={q.rutaNombre} /> : null}
        <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 2 }}>
          <Linea icon="users" texto={plazas} />
          <Text variant="caption" color="inkFaint">
            {etiquetaNivel(q.nivelRecomendado)}
          </Text>
        </View>
      </View>
    </Pressable>
  );
}

function Linea({ icon, texto }: { icon: React.ComponentProps<typeof Feather>["name"]; texto: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: "row", alignItems: "center", gap: 6, flexShrink: 1 }}>
      <Feather name={icon} size={13} color={theme.colors.inkFaint} />
      <Text variant="caption" color="inkMuted" numberOfLines={1} style={{ flexShrink: 1 }}>
        {texto}
      </Text>
    </View>
  );
}

export function EstadoBadge({ estado }: { estado: EstadoQuedada }) {
  const theme = useTheme();
  const cancelada = estado === "cancelada";
  return (
    <View style={{ paddingHorizontal: 8, paddingVertical: 2, borderRadius: theme.radius.full, backgroundColor: cancelada ? theme.colors.dangerSoft : theme.colors.surfaceSunken }}>
      <Text variant="captionMedium" color={cancelada ? "danger" : "inkMuted"} style={{ fontSize: 11 }}>
        {cancelada ? "Cancelada" : estado === "finalizada" ? "Finalizada" : "Programada"}
      </Text>
    </View>
  );
}
