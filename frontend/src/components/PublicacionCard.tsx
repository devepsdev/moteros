import { mediaUrl } from "@/api/config";
import { formatRelativo } from "@/lib/format";
import type { Publicacion } from "@/types/dto";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { Pressable, View } from "react-native";
import { Avatar } from "./ui/Avatar";
import { Text } from "./ui/Text";

interface PublicacionCardProps {
  publicacion: Publicacion;
  onLike: () => void;
  /** En el detalle no se navega al pulsar la tarjeta. */
  enDetalle?: boolean;
}

export function PublicacionCard({ publicacion: p, onLike, enDetalle }: PublicacionCardProps) {
  const theme = useTheme();
  const router = useRouter();
  const imagen = mediaUrl(p.imagenUrl);

  const abrir = () => {
    if (!enDetalle) router.push({ pathname: "/publicacion/[uuid]", params: { uuid: p.uuid } });
  };

  return (
    <View
      style={{
        backgroundColor: theme.colors.surface,
        borderRadius: theme.radius.lg,
        borderWidth: 1,
        borderColor: theme.colors.border,
        overflow: "hidden",
      }}
    >
      <Pressable
        onPress={() => router.push({ pathname: "/usuario/[uuid]", params: { uuid: p.autor.uuid } })}
        style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.md, padding: theme.spacing.lg, paddingBottom: theme.spacing.md }}
      >
        <Avatar nombre={p.autor.nombreCompleto} fotoUrl={p.autor.fotoPerfilUrl} size={38} />
        <View style={{ flex: 1 }}>
          <Text variant="bodyMedium" numberOfLines={1}>
            {p.autor.nombreCompleto}
          </Text>
          <Text variant="caption" color="inkFaint">
            @{p.autor.nombreUsuario} · {formatRelativo(p.fechaPublicacion)}
          </Text>
        </View>
      </Pressable>

      <Pressable onPress={abrir} disabled={enDetalle}>
        <Text variant="body" style={{ paddingHorizontal: theme.spacing.lg, paddingBottom: theme.spacing.md }}>
          {p.contenido}
        </Text>
        {imagen ? (
          <Image source={{ uri: imagen }} style={{ width: "100%", aspectRatio: 4 / 3, backgroundColor: theme.colors.surfaceSunken }} contentFit="cover" transition={150} />
        ) : null}
      </Pressable>

      {p.ruta ? (
        <Pressable
          onPress={() => router.push({ pathname: "/ruta/[uuid]", params: { uuid: p.ruta!.uuid } })}
          style={{
            flexDirection: "row",
            alignItems: "center",
            gap: theme.spacing.sm,
            marginHorizontal: theme.spacing.lg,
            marginTop: theme.spacing.md,
            padding: theme.spacing.md,
            borderRadius: theme.radius.md,
            backgroundColor: theme.colors.accentSoft,
          }}
        >
          <Feather name="map" size={16} color={theme.colors.accent} />
          <Text variant="captionMedium" color="accent" numberOfLines={1} style={{ flex: 1 }}>
            {p.ruta.nombre}
          </Text>
          <Feather name="chevron-right" size={16} color={theme.colors.accent} />
        </Pressable>
      ) : null}

      <View style={{ flexDirection: "row", gap: theme.spacing.xl, padding: theme.spacing.lg, paddingTop: theme.spacing.md }}>
        <Pressable onPress={onLike} hitSlop={8} style={{ flexDirection: "row", alignItems: "center", gap: 6 }} accessibilityRole="button" accessibilityLabel="Me gusta">
          <Feather name="heart" size={18} color={p.likeUsuarioActual ? theme.colors.accent : theme.colors.inkMuted} />
          <Text variant="captionMedium" color={p.likeUsuarioActual ? "accent" : "inkMuted"}>
            {p.numLikes ?? 0}
          </Text>
        </Pressable>
        <Pressable onPress={abrir} disabled={enDetalle} hitSlop={8} style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
          <Feather name="message-circle" size={18} color={theme.colors.inkMuted} />
          <Text variant="captionMedium" color="inkMuted">
            {p.numComentarios ?? 0}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}
