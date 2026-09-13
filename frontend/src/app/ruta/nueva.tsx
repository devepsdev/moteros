import * as rutasApi from "@/api/rutas";
import { OptionPicker } from "@/components/OptionPicker";
import { RutaMap } from "@/components/RutaMap";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { DIFICULTADES, formatKm, longitudTrack, TERRENOS } from "@/lib/format";
import { useTheme } from "@/theme";
import type { Dificultad, PuntoRuta, TipoTerreno } from "@/types/dto";
import * as Location from "expo-location";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/** El backend admite como máximo 7 decimales en las coordenadas. */
const redondear = (grados: number) => Math.round(grados * 1e6) / 1e6;

export default function NuevaRutaScreen() {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [puntos, setPuntos] = useState<PuntoRuta[]>([]);
  const [centro, setCentro] = useState<{ latitud: number; longitud: number } | null>(null);
  const [ubicacionConcedida, setUbicacionConcedida] = useState(false);

  const [nombre, setNombre] = useState("");
  const [puntoInicio, setPuntoInicio] = useState("");
  const [puntoFin, setPuntoFin] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [duracion, setDuracion] = useState("");
  const [dificultad, setDificultad] = useState<Dificultad>("moderada");
  const [terreno, setTerreno] = useState<TipoTerreno>("asfalto");

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Centra el mapa en la ubicación del usuario si da permiso; si no, se queda en España.
  useEffect(() => {
    let activo = true;
    (async () => {
      const { granted } = await Location.requestForegroundPermissionsAsync();
      if (!granted || !activo) return;
      setUbicacionConcedida(true);
      const pos = (await Location.getLastKnownPositionAsync()) ?? (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
      if (pos && activo) setCentro({ latitud: pos.coords.latitude, longitud: pos.coords.longitude });
    })().catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  const distancia = longitudTrack(puntos);

  const anadirPunto = ({ latitud, longitud }: { latitud: number; longitud: number }) =>
    setPuntos((prev) => [...prev, { orden: prev.length, latitud: redondear(latitud), longitud: redondear(longitud) }]);

  const deshacer = () => setPuntos((prev) => prev.slice(0, -1));

  const duracionMin = duracion.trim() ? Number.parseInt(duracion, 10) : undefined;
  const canSubmit = puntos.length >= 2 && nombre.trim() && puntoInicio.trim() && puntoFin.trim() && (duracionMin === undefined || duracionMin > 0);

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    const inicio = puntos[0];
    const fin = puntos[puntos.length - 1];
    try {
      const creada = await rutasApi.crear({
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || null,
        puntoInicio: puntoInicio.trim(),
        latitudInicio: inicio.latitud,
        longitudInicio: inicio.longitud,
        puntoFin: puntoFin.trim(),
        latitudFin: fin.latitud,
        longitudFin: fin.longitud,
        distanciaKm: distancia > 0 ? distancia : null,
        duracionEstimadaMin: duracionMin ?? null,
        dificultad,
        tipoTerreno: terreno,
        puntos,
      });
      router.replace({ pathname: "/ruta/[uuid]", params: { uuid: creada.uuid } });
    } catch (cause) {
      const { message, fields } = describeError(cause);
      setError(message);
      setFieldErrors(fields);
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: theme.colors.background }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
      <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingBottom: insets.bottom + theme.spacing.huge }}>
        <View style={{ height: 420, backgroundColor: theme.colors.surfaceSunken }}>
          <RutaMap puntos={puntos} onAddPunto={anadirPunto} centro={centro} mostrarUbicacion={ubicacionConcedida} />

          <View
            pointerEvents="box-none"
            style={{ position: "absolute", top: insets.top + theme.spacing.sm, left: theme.spacing.md, right: theme.spacing.md, flexDirection: "row", justifyContent: "space-between" }}
          >
            <IconButton name="x" variant="floating" accessibilityLabel="Cancelar" onPress={() => (router.canGoBack() ? router.back() : router.replace("/rutas"))} />
            <View style={{ flexDirection: "row", gap: theme.spacing.sm }}>
              <IconButton name="corner-up-left" variant="floating" accessibilityLabel="Deshacer último punto" disabled={puntos.length === 0} onPress={deshacer} />
              <IconButton name="trash" variant="floating" accessibilityLabel="Borrar recorrido" disabled={puntos.length === 0} onPress={() => setPuntos([])} />
            </View>
          </View>

          <View
            pointerEvents="none"
            style={{
              position: "absolute",
              bottom: theme.spacing.md,
              left: theme.spacing.md,
              right: theme.spacing.md,
              flexDirection: "row",
              alignItems: "center",
              justifyContent: "space-between",
              paddingHorizontal: theme.spacing.lg,
              paddingVertical: theme.spacing.md,
              borderRadius: theme.radius.md,
              backgroundColor: "rgba(14, 15, 17, 0.82)",
            }}
          >
            <Text variant="caption" style={{ color: "#FFFFFF" }}>
              {puntos.length === 0 ? "Toca el mapa para marcar la salida" : puntos.length === 1 ? "Sigue tocando para trazar el recorrido" : `${puntos.length} puntos`}
            </Text>
            <Text variant="title3" style={{ color: "#FFFFFF" }}>
              {formatKm(distancia)}
            </Text>
          </View>
        </View>

        <View style={{ paddingHorizontal: theme.screenPadding, paddingTop: theme.spacing.xl, gap: theme.spacing.lg }}>
          <View style={{ gap: theme.spacing.xs }}>
            <Text variant="overline" color="accent">
              Nueva ruta
            </Text>
            <Text variant="display">Traza tu ruta</Text>
          </View>

          <Input label="Nombre" placeholder="Curvas del Montseny" value={nombre} onChangeText={setNombre} maxLength={120} error={fieldErrors.nombre} />
          <View style={{ flexDirection: "row", gap: theme.spacing.md }}>
            <Input label="Salida" placeholder="Sant Celoni" value={puntoInicio} onChangeText={setPuntoInicio} maxLength={120} error={fieldErrors.puntoInicio} containerStyle={{ flex: 1 }} />
            <Input label="Llegada" placeholder="Viladrau" value={puntoFin} onChangeText={setPuntoFin} maxLength={120} error={fieldErrors.puntoFin} containerStyle={{ flex: 1 }} />
          </View>
          <Input
            label="Duración estimada (min)"
            icon="clock"
            placeholder="90"
            value={duracion}
            onChangeText={(t) => setDuracion(t.replace(/\D/g, ""))}
            keyboardType="number-pad"
            maxLength={4}
            error={fieldErrors.duracionEstimadaMin}
          />
          <OptionPicker label="Dificultad" options={DIFICULTADES} value={dificultad} onChange={setDificultad} />
          <OptionPicker label="Terreno" options={TERRENOS} value={terreno} onChange={setTerreno} />
          <Input
            label="Descripción (opcional)"
            placeholder="Asfalto en buen estado, parada recomendada en…"
            value={descripcion}
            onChangeText={setDescripcion}
            multiline
            maxLength={5000}
            error={fieldErrors.descripcion}
          />

          {error ? (
            <Text variant="caption" color="danger">
              {error}
            </Text>
          ) : null}
          {puntos.length < 2 ? (
            <Text variant="caption" color="inkMuted">
              Marca al menos dos puntos en el mapa para poder guardar.
            </Text>
          ) : null}

          <Button label="Guardar ruta" size="lg" fullWidth loading={submitting} disabled={!canSubmit} onPress={guardar} />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
