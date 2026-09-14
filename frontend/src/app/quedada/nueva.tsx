import * as quedadasApi from "@/api/quedadas";
import { OptionPicker } from "@/components/OptionPicker";
import { RutaMap } from "@/components/RutaMap";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { fechaParaApi, formatDia, formatHora, NIVELES } from "@/lib/format";
import { useTheme } from "@/theme";
import type { NivelRecomendado } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import DateTimePicker, { DateTimePickerAndroid } from "@react-native-community/datetimepicker";
import * as Location from "expo-location";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const redondear = (grados: number) => Math.round(grados * 1e6) / 1e6;

/** Por defecto, el próximo sábado a las 9:00. */
function proximoSabado(): Date {
  const d = new Date();
  d.setDate(d.getDate() + (((6 - d.getDay() + 7) % 7) || 7));
  d.setHours(9, 0, 0, 0);
  return d;
}

export default function NuevaQuedadaScreen() {
  const theme = useTheme();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const params = useLocalSearchParams<{ rutaUuid?: string; rutaNombre?: string }>();

  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [puntoEncuentro, setPuntoEncuentro] = useState("");
  const [ubicacion, setUbicacion] = useState<{ latitud: number; longitud: number } | null>(null);
  const [fecha, setFecha] = useState(proximoSabado);
  const [plazas, setPlazas] = useState("");
  const [nivel, setNivel] = useState<NivelRecomendado>("cualquiera");
  const [ruta, setRuta] = useState(params.rutaUuid ? { uuid: params.rutaUuid, nombre: params.rutaNombre ?? "Ruta" } : null);

  const [centro, setCentro] = useState<{ latitud: number; longitud: number } | null>(null);
  const [ubicacionConcedida, setUbicacionConcedida] = useState(false);
  const [pickerIos, setPickerIos] = useState(false);
  // Hora de referencia fijada al abrir la pantalla (el render ha de ser puro); el backend valida igualmente.
  const [ahora] = useState(() => Date.now());

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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

  /** En Android el selector es un diálogo: primero el día y, al aceptarlo, la hora. */
  const elegirFecha = () => {
    if (Platform.OS !== "android") {
      setPickerIos((v) => !v);
      return;
    }
    DateTimePickerAndroid.open({
      value: fecha,
      mode: "date",
      minimumDate: new Date(),
      onChange: (evento, dia) => {
        if (evento.type !== "set" || !dia) return;
        DateTimePickerAndroid.open({
          value: dia,
          mode: "time",
          is24Hour: true,
          onChange: (eventoHora, hora) => {
            if (eventoHora.type !== "set" || !hora) return;
            const combinada = new Date(dia);
            combinada.setHours(hora.getHours(), hora.getMinutes(), 0, 0);
            setFecha(combinada);
          },
        });
      },
    });
  };

  const plazasNum = plazas ? Number.parseInt(plazas, 10) : undefined;
  const enFuturo = fecha.getTime() > ahora;
  const canSubmit = titulo.trim() && puntoEncuentro.trim() && enFuturo && (plazasNum === undefined || plazasNum > 0);

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const creada = await quedadasApi.crear({
        titulo: titulo.trim(),
        descripcion: descripcion.trim() || null,
        puntoEncuentro: puntoEncuentro.trim(),
        latitudEncuentro: ubicacion?.latitud ?? null,
        longitudEncuentro: ubicacion?.longitud ?? null,
        fechaHora: fechaParaApi(fecha),
        maxParticipantes: plazasNum ?? null,
        nivelRecomendado: nivel,
        rutaUuid: ruta?.uuid ?? null,
      });
      router.replace({ pathname: "/quedada/[uuid]", params: { uuid: creada.uuid } });
    } catch (cause) {
      const { message, fields } = describeError(cause);
      setError(message);
      setFieldErrors(fields);
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: theme.colors.background }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
      <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingTop: insets.top, paddingBottom: insets.bottom + theme.spacing.huge }}>
        <View style={{ paddingHorizontal: theme.spacing.sm }}>
          <IconButton name="x" accessibilityLabel="Cancelar" onPress={() => (router.canGoBack() ? router.back() : router.replace("/quedadas"))} />
        </View>

        <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.lg }}>
          <View style={{ gap: theme.spacing.xs }}>
            <Text variant="overline" color="accent">
              Nueva quedada
            </Text>
            <Text variant="display">Organiza una salida</Text>
          </View>

          <Input label="Título" placeholder="Desayuno motero en Collformic" value={titulo} onChangeText={setTitulo} maxLength={120} error={fieldErrors.titulo} />

          <View style={{ gap: theme.spacing.sm }}>
            <Text variant="overline" color="inkFaint">
              Día y hora
            </Text>
            <Pressable
              onPress={elegirFecha}
              style={({ pressed }) => ({
                flexDirection: "row",
                alignItems: "center",
                gap: theme.spacing.md,
                height: 52,
                paddingHorizontal: theme.spacing.lg,
                borderRadius: theme.radius.md,
                borderWidth: 1,
                borderColor: enFuturo && !fieldErrors.fechaHora ? theme.colors.border : theme.colors.danger,
                backgroundColor: theme.colors.surfaceSunken,
                opacity: pressed ? 0.8 : 1,
              })}
            >
              <Feather name="calendar" size={17} color={theme.colors.accent} />
              <Text variant="bodyMedium" style={{ flex: 1 }}>
                {formatDia(fecha)} · {formatHora(fecha)}
              </Text>
              <Feather name="chevron-down" size={17} color={theme.colors.inkFaint} />
            </Pressable>
            {pickerIos ? (
              <DateTimePicker value={fecha} mode="datetime" display="inline" minimumDate={new Date()} onChange={(_, d) => d && setFecha(d)} themeVariant={theme.isDark ? "dark" : "light"} accentColor={theme.colors.accent} />
            ) : null}
            {!enFuturo || fieldErrors.fechaHora ? (
              <Text variant="caption" color="danger">
                {fieldErrors.fechaHora ?? "La quedada ha de ser en el futuro."}
              </Text>
            ) : null}
          </View>

          <Input label="Punto de encuentro" icon="map-pin" placeholder="Gasolinera de la salida 12" value={puntoEncuentro} onChangeText={setPuntoEncuentro} maxLength={150} error={fieldErrors.puntoEncuentro} />

          <View style={{ gap: theme.spacing.sm }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <Text variant="overline" color="inkFaint">
                Ubicación en el mapa (opcional)
              </Text>
              {ubicacion ? (
                <Pressable onPress={() => setUbicacion(null)} hitSlop={8}>
                  <Text variant="captionMedium" color="accent">
                    Quitar
                  </Text>
                </Pressable>
              ) : null}
            </View>
            <View style={{ height: 220, borderRadius: theme.radius.lg, overflow: "hidden", borderWidth: 1, borderColor: theme.colors.border }}>
              <RutaMap
                puntos={ubicacion ? [{ orden: 0, ...ubicacion }] : []}
                onAddPunto={({ latitud, longitud }) => setUbicacion({ latitud: redondear(latitud), longitud: redondear(longitud) })}
                centro={centro}
                mostrarUbicacion={ubicacionConcedida}
                etiquetaInicio="Punto de encuentro"
              />
            </View>
            <Text variant="caption" color="inkFaint">
              Toca el mapa para marcar dónde os veis.
            </Text>
          </View>

          {ruta ? (
            <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm, padding: theme.spacing.md, borderRadius: theme.radius.md, backgroundColor: theme.colors.accentSoft }}>
              <Feather name="map" size={16} color={theme.colors.accent} />
              <Text variant="captionMedium" color="accent" numberOfLines={1} style={{ flex: 1 }}>
                {ruta.nombre}
              </Text>
              <IconButton name="x" size={16} color={theme.colors.accent} accessibilityLabel="Quitar ruta" onPress={() => setRuta(null)} />
            </View>
          ) : (
            <Text variant="caption" color="inkMuted">
              Para asociar una ruta, abre la ruta y pulsa «Organizar quedada».
            </Text>
          )}

          <Input
            label="Plazas (vacío = sin límite)"
            icon="users"
            placeholder="8"
            value={plazas}
            onChangeText={(t) => setPlazas(t.replace(/\D/g, ""))}
            keyboardType="number-pad"
            maxLength={3}
            error={fieldErrors.maxParticipantes}
          />
          <OptionPicker label="Nivel recomendado" options={NIVELES} value={nivel} onChange={setNivel} />
          <Input label="Detalles (opcional)" placeholder="Ritmo tranquilo, parada para almorzar…" value={descripcion} onChangeText={setDescripcion} multiline maxLength={5000} error={fieldErrors.descripcion} />

          {error ? (
            <Text variant="caption" color="danger">
              {error}
            </Text>
          ) : null}

          <Button label="Publicar quedada" size="lg" fullWidth loading={submitting} disabled={!canSubmit} onPress={guardar} />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
