import * as adminApi from "@/api/admin";
import { useAuth } from "@/auth/AuthContext";
import { UsuarioFila } from "@/components/UsuarioFila";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import type { UsuarioResponse } from "@/types/dto";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { Alert, FlatList, Pressable, View } from "react-native";

export default function AdminUsuariosScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user, refreshProfile } = useAuth();

  const [texto, setTexto] = useState("");
  const [consulta, setConsulta] = useState("");
  const [ocupado, setOcupado] = useState<string | null>(null);

  useEffect(() => {
    const id = setTimeout(() => setConsulta(texto.trim()), 350);
    return () => clearTimeout(id);
  }, [texto]);

  const usuarios = usePagedList((page) => adminApi.usuarios(consulta, page), [consulta]);

  const cambiarRol = (u: UsuarioResponse) => {
    const nuevo = u.rol === "admin" ? "user" : "admin";
    const esYo = u.uuid === user?.uuid;
    const titulo = nuevo === "admin" ? "Dar permisos de administrador" : "Quitar permisos de administrador";
    const mensaje =
      nuevo === "admin"
        ? `${u.nombreCompleto} podrá ver las estadísticas y gestionar los roles de todos los usuarios.`
        : esYo
          ? "Dejarás de tener acceso a la administración."
          : `${u.nombreCompleto} dejará de tener acceso a la administración.`;

    Alert.alert(titulo, mensaje, [
      { text: "Cancelar", style: "cancel" },
      {
        text: nuevo === "admin" ? "Hacer admin" : "Quitar admin",
        style: nuevo === "admin" ? "default" : "destructive",
        onPress: async () => {
          setOcupado(u.uuid);
          try {
            const actualizado = await adminApi.cambiarRol(u.uuid, nuevo);
            usuarios.updateItems((items) => items.map((x) => (x.uuid === u.uuid ? { ...x, rol: actualizado.rol } : x)));
            // Si me quito el rol a mí mismo, al refrescar el perfil se cierra la zona de admin.
            if (esYo) await refreshProfile();
          } catch (cause) {
            Alert.alert("No se ha podido cambiar el rol", describeError(cause).message);
          } finally {
            setOcupado(null);
          }
        },
      },
    ]);
  };

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.xs, paddingLeft: theme.spacing.sm, paddingRight: theme.screenPadding }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={() => (router.canGoBack() ? router.back() : router.replace("/admin"))} />
        <Input icon="search" placeholder="Nombre, usuario o email" value={texto} onChangeText={setTexto} autoCapitalize="none" autoCorrect={false} containerStyle={{ flex: 1 }} />
      </View>

      <View style={{ paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md }}>
        <Text variant="caption" color="inkMuted">
          {usuarios.total != null ? `${usuarios.total.toLocaleString("es-ES")} usuarios` : " "}
        </Text>
      </View>

      {usuarios.loading && usuarios.items.length === 0 ? (
        <LoadingView />
      ) : usuarios.error && usuarios.items.length === 0 ? (
        <ErrorState onRetry={usuarios.reload} />
      ) : (
        <FlatList
          data={usuarios.items}
          keyExtractor={(u) => u.uuid}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, flexGrow: 1 }}
          keyboardShouldPersistTaps="handled"
          onEndReached={usuarios.loadMore}
          onEndReachedThreshold={0.5}
          renderItem={({ item }) => (
            <UsuarioFila usuario={item} detalle={[item.email, item.activo ? null : "inactivo"].filter(Boolean).join(" · ")}>
              <Pressable
                onPress={() => cambiarRol(item)}
                disabled={ocupado === item.uuid}
                accessibilityRole="button"
                accessibilityLabel={item.rol === "admin" ? `Quitar admin a ${item.nombreCompleto}` : `Hacer admin a ${item.nombreCompleto}`}
                style={({ pressed }) => ({
                  paddingHorizontal: theme.spacing.md,
                  height: 30,
                  justifyContent: "center",
                  borderRadius: theme.radius.full,
                  borderWidth: 1,
                  borderColor: item.rol === "admin" ? theme.colors.accent : theme.colors.border,
                  backgroundColor: item.rol === "admin" ? theme.colors.accentSoft : "transparent",
                  opacity: pressed || ocupado === item.uuid ? 0.6 : 1,
                })}
              >
                <Text variant="captionMedium" color={item.rol === "admin" ? "accent" : "inkMuted"}>
                  {item.rol === "admin" ? "Admin" : "Usuario"}
                </Text>
              </Pressable>
            </UsuarioFila>
          )}
          ListEmptyComponent={<EmptyState icon="users" title="Sin resultados" message="Prueba con otro nombre o email." />}
          ListFooterComponent={<ListFooter loadingMore={usuarios.loadingMore} error={usuarios.items.length > 0 ? usuarios.error : null} />}
        />
      )}
    </Screen>
  );
}
