import type { PuntoRuta } from "@/types/dto";
import { decodificarPolilinea } from "@/lib/polilinea";
import { useTheme } from "@/theme";
import { useEffect, useMemo, useRef } from "react";
import { Platform, View, type ViewStyle } from "react-native";
import MapView, { Marker, Polyline, PROVIDER_GOOGLE, type MapPressEvent, type Region } from "react-native-maps";

/** Centro por defecto (península ibérica) cuando no hay puntos que encuadrar. */
export const REGION_ESPANA: Region = { latitude: 40.2, longitude: -3.7, latitudeDelta: 9, longitudeDelta: 9 };

/** Estilo oscuro de Google Maps a juego con la identidad de la app. */
const MAP_STYLE_OSCURO = [
  { elementType: "geometry", stylers: [{ color: "#1b1d20" }] },
  { elementType: "labels.text.fill", stylers: [{ color: "#8a8780" }] },
  { elementType: "labels.text.stroke", stylers: [{ color: "#1b1d20" }] },
  { featureType: "road", elementType: "geometry", stylers: [{ color: "#2c3035" }] },
  { featureType: "road.highway", elementType: "geometry", stylers: [{ color: "#3a3f46" }] },
  { featureType: "water", elementType: "geometry", stylers: [{ color: "#0e1a24" }] },
  { featureType: "poi", stylers: [{ visibility: "off" }] },
  { featureType: "transit", stylers: [{ visibility: "off" }] },
];

interface RutaMapProps {
  puntos: PuntoRuta[];
  /** Si se pasa, el mapa es editable: cada toque añade un punto. */
  onAddPunto?: (punto: { latitud: number; longitud: number }) => void;
  style?: ViewStyle;
  /** Mapa estático (tarjetas): sin gestos. */
  estatico?: boolean;
  /** Muestra el punto azul de la ubicación del usuario. */
  mostrarUbicacion?: boolean;
  /** Centra el mapa en esta posición cada vez que cambia (p. ej. la ubicación del usuario). */
  centro?: { latitud: number; longitud: number } | null;
  /** Título del marcador del primer punto (por defecto "Salida"). */
  etiquetaInicio?: string;
  /**
   * Recorrido por carretera (polilínea codificada). Si falta, la línea une los puntos en
   * recto, que es lo que pasa con los tracks GPX (ya siguen la carretera) o sin conexión.
   */
  trazado?: string | null;
}

export function RutaMap({ puntos, onAddPunto, style, estatico, mostrarUbicacion, centro, etiquetaInicio = "Salida", trazado }: RutaMapProps) {
  const theme = useTheme();
  const mapRef = useRef<MapView>(null);
  const coords = puntos.map((p) => ({ latitude: p.latitud, longitude: p.longitud }));
  const linea = useMemo(() => (trazado ? decodificarPolilinea(trazado) : null), [trazado]);
  const recorrido = linea && linea.length > 1 ? linea : coords;

  // Encuadra el recorrido cada vez que cambia (solo en modo lectura).
  const numPuntos = recorrido.length;
  useEffect(() => {
    if (onAddPunto || numPuntos < 2) return;
    const id = setTimeout(() => {
      mapRef.current?.fitToCoordinates(recorrido, {
        edgePadding: { top: 48, right: 48, bottom: 48, left: 48 },
        animated: false,
      });
    }, 250);
    return () => clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numPuntos, onAddPunto]);

  useEffect(() => {
    if (!centro) return;
    mapRef.current?.animateToRegion(
      { latitude: centro.latitud, longitude: centro.longitud, latitudeDelta: 0.25, longitudeDelta: 0.25 },
      400
    );
  }, [centro]);

  const regionInicial: Region =
    coords.length > 0
      ? { latitude: coords[0].latitude, longitude: coords[0].longitude, latitudeDelta: 0.4, longitudeDelta: 0.4 }
      : REGION_ESPANA;

  const onPress = (e: MapPressEvent) => {
    if (!onAddPunto) return;
    const { latitude, longitude } = e.nativeEvent.coordinate;
    onAddPunto({ latitud: latitude, longitud: longitude });
  };

  return (
    <MapView
      ref={mapRef}
      style={[{ flex: 1 }, style]}
      provider={Platform.OS === "android" ? PROVIDER_GOOGLE : undefined}
      initialRegion={regionInicial}
      customMapStyle={theme.isDark ? MAP_STYLE_OSCURO : undefined}
      onPress={onPress}
      scrollEnabled={!estatico}
      zoomEnabled={!estatico}
      rotateEnabled={false}
      pitchEnabled={false}
      toolbarEnabled={false}
      showsUserLocation={mostrarUbicacion}
      showsMyLocationButton={mostrarUbicacion}
    >
      {recorrido.length > 1 ? <Polyline coordinates={recorrido} strokeColor={theme.colors.track} strokeWidth={5} /> : null}
      {/* Al trazar, los puntos intermedios que se van marcando. */}
      {onAddPunto
        ? coords.slice(1, -1).map((c, i) => (
            <Marker key={i} coordinate={c} anchor={{ x: 0.5, y: 0.5 }} tracksViewChanges={false}>
              <View style={{ width: 12, height: 12, borderRadius: 6, backgroundColor: theme.colors.track, borderWidth: 2, borderColor: "#FFFFFF" }} />
            </Marker>
          ))
        : null}
      {coords.length > 0 ? <Marker coordinate={coords[0]} pinColor={etiquetaInicio === "Salida" ? "green" : theme.colors.accent} title={etiquetaInicio} /> : null}
      {coords.length > 1 ? <Marker coordinate={coords[coords.length - 1]} pinColor="red" title="Llegada" /> : null}
    </MapView>
  );
}
