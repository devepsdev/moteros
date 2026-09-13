import { ApiError } from "@/api/client";
import * as usuariosApi from "@/api/usuarios";
import { useAuth } from "@/auth/AuthContext";
import { PerfilView } from "@/components/PerfilView";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import { Redirect, useLocalSearchParams, useRouter } from "expo-router";
import { ScrollView } from "react-native";

export default function UsuarioScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { uuid } = useLocalSearchParams<{ uuid: string }>();
  const { user } = useAuth();
  const usuario = useAsync(() => usuariosApi.obtener(uuid), [uuid]);

  // El perfil propio vive en su pestaña.
  if (uuid === user?.uuid) return <Redirect href="/perfil" />;

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/"));
  const noExiste = usuario.error instanceof ApiError && usuario.error.status === 404;

  return (
    <Screen>
      <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={volver} style={{ marginLeft: theme.spacing.sm }} />
      {usuario.data ? (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingTop: theme.spacing.sm, paddingBottom: theme.spacing.huge }}>
          <PerfilView usuario={usuario.data} esPropio={false} />
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
