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
import { useUbicacion } from "@/lib/ubicacion";
import { usePagedList } from "@/lib/usePagedList";
import { useRefocus } from "@/lib/useRefocus";
import { useTheme } from "@/theme";
import type { Dificultad, TipoTerreno } from "@/types/dto";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { Alert, FlatList, RefreshControl, ScrollView, View } from "react-native";

/** Dos decimales de grado: en torno a un kilómetro, suficiente para ordenar por cercanía. */
const aproximar = (grados: number) => Math.round(grados * 100) / 100;

export default function RutasScreen() {
  const theme = useTheme();
  const router = useRouter();

  const [texto, setTexto] = useState("");
  const [nombre, setNombre] = useState("");
  const [dificultad, setDificultad] = useState<Dificultad | undefined>();
  const [terreno, setTerreno] = useState<TipoTerreno | undefined>();
  const [cerca, setCerca] = useState(false);
  const ubicacion = useUbicacion();

  // Radio amplio: el catálogo está repartido y con menos no saldría nada en muchas provincias.
  const RADIO_KM = 200;

  const alternarCerca = async () => {
    if (cerca) {
      setCerca(false);
      return;
    }
    const pos = await ubicacion.pedir();
    if (!pos) {
      Alert.alert(
        "Sin ubicación",
        "Para ordenar las rutas por cercanía hace falta el permiso de ubicación. Puedes dárselo desde los ajustes del móvil."
      );
      return;
    }
    setCerca(true);
  };

  // Espera a que se deje de escribir antes de buscar.
  useEffect(() => {
    const id = setTimeout(() => setNombre(texto.trim()), 350);
    return () => clearTimeout(id);
  }, [texto]);

  const desde = cerca ? ubicacion.coords : null;
  const rutas = usePagedList(
    (page) =>
      rutasApi.listar(
        {
          nombre: nombre || undefined,
          dificultad,
          tipoTerreno: terreno,
          // Al servidor solo va una ubicación aproximada (unos 1 km); la precisa se queda en el
          // móvil para calcular el «a X km» de cada tarjeta.
          ...(desde ? { latitud: aproximar(desde.latitud), longitud: aproximar(desde.longitud), radioKm: RADIO_KM } : {}),
        },
        page
      ),
    [nombre, dificultad, terreno, desde?.latitud, desde?.longitud]
  );

  useRefocus(rutas.reload);

  const hayFiltros = Boolean(nombre || dificultad || terreno || desde);

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
        // ScrollView encoge por defecto (flexShrink 1): sin esto, la lista le come la altura y corta los filtros.
        style={{ flexGrow: 0, flexShrink: 0 }}
        contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingVertical: theme.spacing.md, gap: theme.spacing.sm }}
      >
        <Chip
          label={ubicacion.cargando ? "Buscándote…" : "Cerca de mí"}
          icon="map-pin"
          selected={cerca}
          onPress={alternarCerca}
        />
        <View style={{ width: 1, backgroundColor: theme.colors.border, marginHorizontal: theme.spacing.xs }} />
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
          renderItem={({ item }) => <RutaCard ruta={item} desde={desde} />}
          contentContainerStyle={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.md, paddingBottom: theme.spacing.xl, gap: theme.spacing.md, flexGrow: 1 }}
          keyboardShouldPersistTaps="handled"
          onEndReached={rutas.loadMore}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={rutas.loading && rutas.items.length > 0} onRefresh={rutas.reload} tintColor={theme.colors.accent} colors={[theme.colors.accent]} progressBackgroundColor={theme.colors.surface} />}
          ListEmptyComponent={
            hayFiltros ? (
              <EmptyState
                icon="search"
                title="Sin resultados"
                message={
                  desde
                    ? `No hay rutas a menos de ${RADIO_KM} km de donde estás. Quita el filtro de cercanía para verlas todas.`
                    : "Prueba con otro nombre o quita algún filtro."
                }
              />
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
