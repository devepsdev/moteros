import * as usuariosApi from "@/api/usuarios";
import { useAuth } from "@/auth/AuthContext";
import { UsuarioFila } from "@/components/UsuarioFila";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { usePagedList } from "@/lib/usePagedList";
import { useTheme } from "@/theme";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { FlatList, View } from "react-native";

export default function BuscarUsuariosScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user } = useAuth();

  const [texto, setTexto] = useState("");
  const [consulta, setConsulta] = useState("");

  useEffect(() => {
    const id = setTimeout(() => setConsulta(texto.trim()), 350);
    return () => clearTimeout(id);
  }, [texto]);

  const resultados = usePagedList((page) => usuariosApi.buscar(consulta, page), [consulta]);
  const items = resultados.items.filter((u) => u.uuid !== user?.uuid);

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.xs, paddingLeft: theme.spacing.sm, paddingRight: theme.screenPadding, paddingBottom: theme.spacing.md }}>
        <IconButton name="arrow-left" accessibilityLabel="Volver" onPress={() => (router.canGoBack() ? router.back() : router.replace("/"))} />
        <Input
          icon="search"
          placeholder="Nombre, usuario o ciudad"
          value={texto}
          onChangeText={setTexto}
          autoFocus
          autoCapitalize="none"
          autoCorrect={false}
          returnKeyType="search"
          containerStyle={{ flex: 1 }}
        />
      </View>

      {resultados.loading && resultados.items.length === 0 ? (
        <LoadingView />
      ) : resultados.error && resultados.items.length === 0 ? (
        <ErrorState onRetry={resultados.reload} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(u) => u.uuid}
          renderItem={({ item }) => <UsuarioFila usuario={item} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, flexGrow: 1 }}
          keyboardShouldPersistTaps="handled"
          onEndReached={resultados.loadMore}
          onEndReachedThreshold={0.5}
          ListEmptyComponent={
            <EmptyState icon="search" title={consulta ? "Nadie con ese nombre" : "Encuentra moteros"} message={consulta ? "Prueba con el nombre de usuario o la ciudad." : "Busca por nombre, usuario o ciudad."} />
          }
          ListFooterComponent={<ListFooter loadingMore={resultados.loadingMore} error={resultados.items.length > 0 ? resultados.error : null} />}
        />
      )}
    </Screen>
  );
}
