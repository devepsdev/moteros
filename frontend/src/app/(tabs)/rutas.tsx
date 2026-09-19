import * as rutasApi from "@/api/rutas";
import { RutaCard } from "@/components/RutaCard";
import { Chip } from "@/components/ui/Chip";
import { EmptyState, ErrorState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { ListFooter, LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { DIFICULTADES, TERRENOS } from "@/lib/format";
import { usePagedList } from "@/lib/usePagedList";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import type { Dificultad, TipoTerreno } from "@/types/dto";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { FlatList, RefreshControl, ScrollView, View } from "react-native";

export default function RutasScreen() {
  const theme = useTheme();
  const router = useRouter();

  const [texto, setTexto] = useState("");
  const [nombre, setNombre] = useState("");
  const [dificultad, setDificultad] = useState<Dificultad | undefined>();
  const [terreno, setTerreno] = useState<TipoTerreno | undefined>();

  // Espera a que se deje de escribir antes de buscar.
  useEffect(() => {
    const id = setTimeout(() => setNombre(texto.trim()), 350);
    return () => clearTimeout(id);
  }, [texto]);

  const rutas = usePagedList(
    (page) => rutasApi.listar({ nombre: nombre || undefined, dificultad, tipoTerreno: terreno }, page),
    [nombre, dificultad, terreno]
  );

  useRefocus(rutas.reload);

  const hayFiltros = Boolean(nombre || dificultad || terreno);

  return (
    <Screen>
      <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md }}>
        <View>
          <Text variant="overline" color="accent">
            Explora
          </Text>
          <Text variant="display">Rutas</Text>
        </View>
        <IconButton name="plus" variant="accent" accessibilityLabel="Crear ruta" onPress={() => router.push("/ruta/nueva")} />
      </View>

      <View style={{ paddingHorizontal: theme.screenPadding }}>
        <Input icon="search" placeholder="Buscar por nombre" value={texto} onChangeText={setTexto} autoCorrect={false} returnKeyType="search" />
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={{ flexGrow: 0 }}
        contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md, gap: theme.spacing.sm }}
      >
        {DIFICULTADES.map((d) => (
          <Chip key={d.value} label={d.label} selected={dificultad === d.value} onPress={() => setDificultad(dificultad === d.value ? undefined : d.value)} />
        ))}
        <View style={{ width: 1, backgroundColor: theme.colors.border, marginHorizontal: theme.spacing.xs }} />
        {TERRENOS.map((t) => (
          <Chip key={t.value} label={t.label} selected={terreno === t.value} onPress={() => setTerreno(terreno === t.value ? undefined : t.value)} />
        ))}
      </ScrollView>

      {rutas.loading && rutas.items.length === 0 ? (
        <LoadingView />
      ) : rutas.error && rutas.items.length === 0 ? (
        <ErrorState onRetry={rutas.reload} />
      ) : (
        <FlatList
          data={rutas.items}
          keyExtractor={(r) => r.uuid}
          renderItem={({ item }) => <RutaCard ruta={item} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.md, paddingBottom: theme.spacing.xl, gap: theme.spacing.md, flexGrow: 1 }}
          keyboardShouldPersistTaps="handled"
          onEndReached={rutas.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={rutas.loading && rutas.items.length > 0} onRefresh={rutas.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          ListEmptyComponent={
            hayFiltros ? (
              <EmptyState icon="search" title="Sin resultados" message="Prueba con otro nombre o quita algún filtro." />
            ) : (
              <EmptyState
                icon="map"
                title="Aún no hay rutas"
                message="Traza la primera: marca los puntos en el mapa y compártela con la comunidad."
                actionLabel="Crear ruta"
                onAction={() => router.push("/ruta/nueva")}
              />
            )
          }
          ListFooterComponent={<ListFooter loadingMore={rutas.loadingMore} error={rutas.items.length > 0 ? rutas.error : null} />}
        />
      )}
    </Screen>
  );
}
