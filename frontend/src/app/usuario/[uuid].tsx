import * as bloqueosApi from "@/api/bloqueos";
import { ApiError } from "@/api/client";
import * as usuariosApi from "@/api/usuarios";
import { useAuth } from "@/auth/AuthContext";
import { AmistadAcciones } from "@/components/AmistadAcciones";
import { PerfilView } from "@/components/PerfilView";
import { Button } from "@/components/ui/Button";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { confirmarBloqueo } from "@/lib/bloqueo";
import { abrirDenuncia } from "@/lib/denuncia";
import { describeError } from "@/lib/errors";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import { Redirect, useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert, ScrollView, View } from "react-native";

export default function UsuarioScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { user } = useAuth();
  const usuario = useAsync(() => usuariosApi.obtener(uuid), [uuid]);
  const bloqueado = useAsync(() => bloqueosApi.heBloqueado(uuid), [uuid]);
  const [desbloqueando, setDesbloqueando] = useState(false);
  // Al cambiar el bloqueo se recargan las publicaciones y demás contenido del perfil.
  const [reloadKey, setReloadKey] = useState(0);

  // El perfil propio vive en su pestaña.
  if (uuid === user?.uuid) return <Redirect href="/perfil" />;

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/"));
  const noExiste = usuario.error instanceof ApiError && usuario.error.status === 404;

  const recargar = () => {
    bloqueado.reload();
    usuario.reload();
    setReloadKey((k) => k + 1);
  };

  const desbloquear = async () => {
    setDesbloqueando(true);
    try {
      await bloqueosApi.desbloquear(uuid);
      recargar();
    } catch (cause) {
      Alert.alert("No se ha podido desbloquear", describeError(cause).message);
    } finally {
      setDesbloqueando(false);
    }
  };

  const u = usuario.data;
  const menu = () => {
    if (!u) return;
    const autor = { uuid: u.uuid, nombre: u.nombreCompleto };
    Alert.alert(u.nombreCompleto, undefined, [
      { text: "Cancelar", style: "cancel" },
      { text: "Denunciar perfil", onPress: () => abrirDenuncia(router, "usuario", u.uuid, bloqueado.data ? undefined : autor) },
      bloqueado.data ? { text: "Desbloquear", onPress: desbloquear } : { text: "Bloquear", style: "destructive", onPress: () => confirmarBloqueo(autor, recargar) },
    ]);
  };

  return (
    <Screen>
      <View style={{ flexDirection: "row", justifyContent: "space-between", paddingHorizontal: theme.spacing.sm }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} />
        {u && bloqueado.data !== null ? <IconButton name="more-vertical" accessibilityLabel="Más opciones" onPress={menu} /> : null}
      </View>
      {u ? (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingTop: theme.spacing.sm, paddingBottom: theme.spacing.huge }}>
          <PerfilView
            reloadKey={reloadKey}
            usuario={u}
            esPropio={false}
            acciones={
              bloqueado.data ? (
                <View style={{ gap: theme.spacing.sm }}>
                  <Text variant="body" color="inkMuted">
                    Has bloqueado a este usuario: no podéis escribiros ni veis el contenido del otro.
                  </Text>
                  <Button label="Desbloquear" variant="secondary" loading={desbloqueando} onPress={desbloquear} />
                </View>
              ) : (
                <AmistadAcciones usuario={u} onCambio={usuario.reload} />
              )
            }
          />
        </ScrollView>
      ) : usuario.loading ? (
        <LoadingView />
      ) : noExiste ? (
        <EmptyState icon="user-x" title="Usuario no encontrado" message="Puede que haya eliminado su cuenta." />
      ) : (
        <ErrorState onRetry={usuario.reload} />
      )}
    </Screen>
  );
}
