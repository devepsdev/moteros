import * as quedadasApi from "@/api/quedadas";
import { useAuth } from "@/auth/AuthContext";
import { QuedadaCard } from "@/components/QuedadaCard";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Segmented } from "@/components/ui/Segmented";
import { Text } from "@/components/ui/Text";
import { usePagedList } from "@/lib/usePagedList";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import { useRouter } from "expo-router";
import { useState } from "react";
import { FlatList, RefreshControl, View } from "react-native";

type Vista = "proximas" | "apuntado" | "organizo";

const VACIOS: Record<Vista, { icon: "calendar" | "check-circle" | "flag"; title: string; message: string }> = {
  proximas: { icon: "calendar", title: "No hay quedadas a la vista", message: "Organiza la primera: elige ruta, punto de encuentro y hora." },
  apuntado: { icon: "check-circle", title: "Aún no te has apuntado a ninguna", message: "Échale un ojo a las próximas quedadas y súmate." },
  organizo: { icon: "flag", title: "No has organizado ninguna", message: "Monta una salida y avisa a la comunidad." },
};

export default function QuedadasScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user } = useAuth();
  const [vista, setVista] = useState<Vista>("proximas");

  const lista = usePagedList(
    (page) =>
      vista === "proximas"
        ? quedadasApi.proximas(page)
        : vista === "apuntado"
          ? quedadasApi.misInscripciones(page)
          : quedadasApi.deOrganizador(user?.uuid ?? "", page),
    [vista, user?.uuid]
  );

  useRefocus(lista.reload);

  const vacio = VACIOS[vista];

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md }}>
        <View>
          <Text variant="overline" color="accent">
            Rodar juntos
          </Text>
          <Text variant="display">Quedadas</Text>
        </View>
        <IconButton name="plus" variant="accent" accessibilityLabel="Organizar quedada" onPress={() => router.push("/quedada/nueva")} />
      </View>

      <Segmented
        style={{ marginHorizontal: theme.screenPadding, marginBottom: theme.spacing.md }}
        value={vista}
        onChange={setVista}
        options={[
          { value: "proximas", label: "Próximas" },
          { value: "apuntado", label: "Me apunté" },
          { value: "organizo", label: "Organizo" },
        ]}
      />

      {lista.loading && lista.items.length === 0 ? (
        <LoadingView />
      ) : lista.error && lista.items.length === 0 ? (
        <ErrorState onRetry={lista.reload} />
      ) : (
        <FlatList
          data={lista.items}
          keyExtractor={(q) => q.uuid}
          renderItem={({ item }) => <QuedadaCard quedada={item} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.md, flexGrow: 1 }}
          onEndReached={lista.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={lista.loading && lista.items.length > 0} onRefresh={lista.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          ListEmptyComponent={
            <EmptyState
              icon={vacio.icon}
              title={vacio.title}
              message={vacio.message}
              actionLabel={vista === "apuntado" ? "Ver próximas" : "Organizar quedada"}
              onAction={() => (vista === "apuntado" ? setVista("proximas") : router.push("/quedada/nueva"))}
            />
          }
          ListFooterComponent={<ListFooter loadingMore={lista.loadingMore} error={lista.items.length > 0 ? lista.error : null} />}
        />
      )}
    </Screen>
  );
}
