import { mediaUrl } from "@/api/config";
import * as publicacionesApi from "@/api/publicaciones";
import { FormScreen } from "@/components/FormScreen";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { elegirYSubirImagen } from "@/lib/pickImage";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Image } from "expo-image";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Alert, Pressable, View } from "react-native";

export default function NuevaPublicacionScreen() {
  const theme = useTheme();
  const router = useRouter();
  const params = useLocalSearchParams<{ rutaUuid?: string; rutaNombre?: string }>();

  const [contenido, setContenido] = useState("");
  const [imagenUrl, setImagenUrl] = useState<string | null>(null);
  const [rutaAdjunta, setRutaAdjunta] = useState(params.rutaUuid ? { uuid: params.rutaUuid, nombre: params.rutaNombre ?? "Ruta" } : null);
  const [subiendo, setSubiendo] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const elegirImagen = async () => {
    setSubiendo(true);
    setError(null);
    try {
      const url = await elegirYSubirImagen([4, 3]);
      if (url) setImagenUrl(url);
    } catch (cause) {
      Alert.alert("No se ha podido subir la imagen", describeError(cause).message);
    } finally {
      setSubiendo(false);
    }
  };

  const publicar = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await publicacionesApi.crear({ contenido: contenido.trim(), imagenUrl, rutaUuid: rutaAdjunta?.uuid ?? null });
      if (router.canGoBack()) router.back();
      else router.replace("/");
    } catch (cause) {
      setError(describeError(cause).message);
      setSubmitting(false);
    }
  };

  const preview = mediaUrl(imagenUrl);

  return (
    <FormScreen eyebrow="Nueva publicación" title="¿Qué tal la salida?" fallbackHref="/">
      <Input placeholder="Cuéntaselo a la comunidad…" value={contenido} onChangeText={setContenido} multiline maxLength={5000} autoFocus />

      {rutaAdjunta ? (
        <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm, padding: theme.spacing.md, borderRadius: theme.radius.md, backgroundColor: theme.colors.accentSoft }}>
          <Feather name="map" size={16} color={theme.colors.accent} />
          <Text variant="captionMedium" color="accent" numberOfLines={1} style={{ flex: 1 }}>
            {rutaAdjunta.nombre}
          </Text>
          <IconButton name="x" size={16} color={theme.colors.accent} accessibilityLabel="Quitar ruta" onPress={() => setRutaAdjunta(null)} />
        </View>
      ) : null}

      {preview ? (
        <View style={{ borderRadius: theme.radius.lg, overflow: "hidden" }}>
          <Image source={{ uri: preview }} style={{ width: "100%", aspectRatio: 4 / 3 }} contentFit="cover" />
          <IconButton name="x" variant="floating" accessibilityLabel="Quitar imagen" onPress={() => setImagenUrl(null)} style={{ position: "absolute", top: theme.spacing.sm, right: theme.spacing.sm }} />
        </View>
      ) : (
        <Pressable
          onPress={elegirImagen}
          disabled={subiendo}
          style={({ pressed }) => ({
            height: 110,
            borderRadius: theme.radius.lg,
            borderWidth: 1,
            borderStyle: "dashed",
            borderColor: theme.colors.borderStrong,
            alignItems: "center",
            justifyContent: "center",
            gap: theme.spacing.sm,
            opacity: pressed ? 0.7 : 1,
          })}
        >
          {subiendo ? (
            <ActivityIndicator color={theme.colors.accent} />
          ) : (
            <>
              <Feather name="image" size={22} color={theme.colors.inkMuted} />
              <Text variant="captionMedium" color="inkMuted">
                Añadir foto
              </Text>
            </>
          )}
        </Pressable>
      )}

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label="Publicar" size="lg" fullWidth loading={submitting} disabled={contenido.trim().length === 0 || subiendo} onPress={publicar} />
    </FormScreen>
  );
}
