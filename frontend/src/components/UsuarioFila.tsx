import type { UsuarioSummary } from "@/types/dto";
import { useTheme } from "@/theme";
import { useRouter } from "expo-router";
import type { ReactNode } from "react";
import { Pressable, View } from "react-native";
import { Avatar } from "./ui/Avatar";
import { Text } from "./ui/Text";

interface UsuarioFilaProps {
  usuario: UsuarioSummary;
  /** Texto bajo el nombre; por defecto @usuario · ciudad. */
  detalle?: string;
  /** Acciones a la derecha (botones de aceptar, etc.). */
  children?: ReactNode;
  onPress?: () => void;
}

/** Fila de usuario para listas: avatar, nombre y acciones. Por defecto abre su perfil. */
export function UsuarioFila({ usuario, detalle, children, onPress }: UsuarioFilaProps) {
  const theme = useTheme();
  const router = useRouter();
  const texto = detalle ?? [`@${usuario.nombreUsuario}`, usuario.ciudad].filter(Boolean).join(" · ");

  return (
    <Pressable
      onPress={onPress ?? (() => router.push({ pathname: "/usuario/[uuid]", params: { uuid: usuario.uuid } }))}
      style={({ pressed }) => ({
        flexDirection: "row",
        alignItems: "center",
        gap: theme.spacing.md,
        paddingVertical: theme.spacing.sm,
        opacity: pressed ? 0.7 : 1,
      })}
    >
      <Avatar nombre={usuario.nombreCompleto} fotoUrl={usuario.fotoPerfilUrl} size={44} />
      <View style={{ flex: 1, gap: 1 }}>
        <Text variant="bodyMedium" numberOfLines={1}>
          {usuario.nombreCompleto}
        </Text>
        <Text variant="caption" color="inkFaint" numberOfLines={1}>
          {texto}
        </Text>
      </View>
      {children ? <View style={{ flexDirection: "row", gap: theme.spacing.sm }}>{children}</View> : null}
    </Pressable>
  );
}
