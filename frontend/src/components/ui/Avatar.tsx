import { mediaUrl } from "@/api/config";
import { iniciales } from "@/lib/format";
import { useTheme } from "@/theme";
import { Image } from "expo-image";
import { View } from "react-native";
import { Text } from "./Text";

export function Avatar({ nombre, fotoUrl, size = 40 }: { nombre: string; fotoUrl?: string | null; size?: number }) {
  const theme = useTheme();
  const uri = mediaUrl(fotoUrl);

  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        overflow: "hidden",
        backgroundColor: theme.colors.accentSoft,
        alignItems: "center",
        justifyContent: "center",
        borderWidth: 1,
        borderColor: theme.colors.border,
      }}
    >
      {uri ? (
        <Image source={{ uri }} style={{ width: size, height: size }} contentFit="cover" transition={150} />
      ) : (
        <Text variant="title3" color="accent" style={{ fontSize: size * 0.4, lineHeight: size * 0.48 }}>
          {iniciales(nombre)}
        </Text>
      )}
    </View>
  );
}
