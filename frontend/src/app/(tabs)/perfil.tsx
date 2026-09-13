import { useAuth } from "@/auth/AuthContext";
import { PerfilView } from "@/components/PerfilView";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter, type Href } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, RefreshControl, ScrollView, View } from "react-native";

export default function PerfilScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user, refreshProfile, logout } = useAuth();
  const [reloadKey, setReloadKey] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  // Al volver de editar el perfil o una moto se refrescan datos y contadores.
  useRefocus(() => {
    refreshProfile();
    setReloadKey((k) => k + 1);
  });

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshProfile();
    setReloadKey((k) => k + 1);
    setRefreshing(false);
  };

  const confirmarLogout = () =>
    Alert.alert("Cerrar sesión", "¿Quieres cerrar la sesión en este dispositivo?", [
      { text: "Cancelar", style: "cancel" },
      { text: "Cerrar sesión", style: "destructive", onPress: () => logout() },
    ]);

  if (!user) return null;

  const opciones: { icon: React.ComponentProps<typeof Feather>["name"]; label: string; href?: Href; onPress?: () => void; danger?: boolean }[] = [
    { icon: "user", label: "Datos personales", href: "/cuenta/datos" },
    { icon: "lock", label: "Cambiar contraseña", href: "/cuenta/contrasena" },
    { icon: "log-out", label: "Cerrar sesión", onPress: confirmarLogout },
    { icon: "trash-2", label: "Eliminar cuenta", href: "/cuenta/eliminar", danger: true },
  ];

  return (
    <Screen>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: theme.spacing.huge, gap: theme.spacing.xxl }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
      >
        <View style={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.md }}>
          <Text variant="overline" color="accent">
            Tu perfil
          </Text>
        </View>

        <PerfilView usuario={user} esPropio reloadKey={reloadKey} />

        <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.md }}>
          <Text variant="title2">Cuenta</Text>
          <View style={{ backgroundColor: theme.colors.surface, borderRadius: theme.radius.lg, borderWidth: 1, borderColor: theme.colors.border, overflow: "hidden" }}>
            {opciones.map((o, i) => (
              <Pressable
                key={o.label}
                onPress={o.onPress ?? (() => o.href && router.push(o.href))}
                style={({ pressed }) => ({
                  flexDirection: "row",
                  alignItems: "center",
                  gap: theme.spacing.md,
                  paddingHorizontal: theme.spacing.lg,
                  height: 54,
                  borderTopWidth: i === 0 ? 0 : 1,
                  borderTopColor: theme.colors.border,
                  backgroundColor: pressed ? theme.colors.surfaceSunken : "transparent",
                })}
              >
                <Feather name={o.icon} size={18} color={o.danger ? theme.colors.danger : theme.colors.inkMuted} />
                <Text variant="bodyMedium" color={o.danger ? "danger" : "ink"} style={{ flex: 1 }}>
                  {o.label}
                </Text>
                {o.href ? <Feather name="chevron-right" size={18} color={theme.colors.inkFaint} /> : null}
              </Pressable>
            ))}
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}
