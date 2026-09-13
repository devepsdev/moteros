import { mediaUrl } from "@/api/config";
import { etiquetaTipoMoto } from "@/lib/format";
import type { MotoResponse } from "@/types/dto";
import { useTheme } from "@/theme";
import MaterialCommunityIcons from "@expo/vector-icons/MaterialCommunityIcons";
import { Image } from "expo-image";
import { Pressable, View } from "react-native";
import { Text } from "./ui/Text";

export function MotoCard({ moto, onPress }: { moto: MotoResponse; onPress?: () => void }) {
  const theme = useTheme();
  const foto = mediaUrl(moto.fotoUrl);
  const detalle = [moto.anio, moto.cilindradaCc ? `${moto.cilindradaCc} cc` : null, etiquetaTipoMoto(moto.tipo)]
    .filter(Boolean)
    .join(" · ");

  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      style={({ pressed }) => ({
        width: 170,
        backgroundColor: theme.colors.surface,
        borderRadius: theme.radius.lg,
        borderWidth: 1,
        borderColor: theme.colors.border,
        overflow: "hidden",
        opacity: pressed ? 0.85 : 1,
      })}
    >
      <View style={{ height: 100, backgroundColor: theme.colors.surfaceSunken, alignItems: "center", justifyContent: "center" }}>
        {foto ? (
          <Image source={{ uri: foto }} style={{ width: "100%", height: "100%" }} contentFit="cover" />
        ) : (
          <MaterialCommunityIcons name="motorbike" size={42} color={theme.colors.inkFaint} />
        )}
      </View>
      <View style={{ padding: theme.spacing.md, gap: 2 }}>
        <Text variant="title3" numberOfLines={1}>
          {moto.marca} {moto.modelo}
        </Text>
        <Text variant="caption" color="inkMuted" numberOfLines={1}>
          {detalle}
        </Text>
      </View>
    </Pressable>
  );
}
