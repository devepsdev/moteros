import { formatDuracion, formatKm } from "@/lib/format";
import { useTheme } from "@/theme";
import type { AlternativaTramo } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { Pressable, ScrollView, View } from "react-native";
import { Text } from "./ui/Text";

interface AlternativasTramoProps {
  opciones: AlternativaTramo[];
  elegida: number;
  onElegir: (indice: number) => void;
}

/** Tarjetas para elegir la carretera de un tramo: km, tiempo y cuál tiene más curvas. */
export function AlternativasTramo({ opciones, elegida, onElegir }: AlternativasTramoProps) {
  const theme = useTheme();
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: theme.spacing.sm }}>
      {opciones.map((o, i) => {
        const activa = i === elegida;
        return (
          <Pressable
            key={i}
            onPress={() => onElegir(i)}
            accessibilityRole="radio"
            accessibilityState={{ selected: activa }}
            style={({ pressed }) => ({
              minWidth: 132,
              padding: theme.spacing.md,
              gap: 4,
              borderRadius: theme.radius.md,
              borderWidth: activa ? 2 : 1,
              borderColor: activa ? theme.colors.accent : theme.colors.border,
              backgroundColor: activa ? theme.colors.accentSoft : theme.colors.surface,
              opacity: pressed ? 0.8 : 1,
            })}
          >
            <Text variant="overline" color={activa ? "accent" : "inkFaint"}>
              Opción {i + 1}
            </Text>
            <Text variant="title3">{formatKm(o.distanciaKm)}</Text>
            <Text variant="caption" color="inkMuted">
              {formatDuracion(o.duracionMin)}
            </Text>
            {o.masCurvas ? (
              <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
                <Feather name="activity" size={13} color={theme.colors.accent} />
                <Text variant="captionMedium" color="accent">
                  Más curvas
                </Text>
              </View>
            ) : null}
            {o.masRapida ? (
              <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
                <Feather name="zap" size={13} color={theme.colors.inkMuted} />
                <Text variant="captionMedium" color="inkMuted">
                  Más rápida
                </Text>
              </View>
            ) : null}
          </Pressable>
        );
      })}
    </ScrollView>
  );
}
