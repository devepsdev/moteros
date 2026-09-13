import * as usuariosApi from "@/api/usuarios";
import { useAuth } from "@/auth/AuthContext";
import { FormScreen } from "@/components/FormScreen";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { elegirYSubirImagen } from "@/lib/pickImage";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Pressable, View } from "react-native";

export default function DatosScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { user, refreshProfile } = useAuth();

  const [nombreUsuario, setNombreUsuario] = useState(user?.nombreUsuario ?? "");
  const [nombreCompleto, setNombreCompleto] = useState(user?.nombreCompleto ?? "");
  const [ciudad, setCiudad] = useState(user?.ciudad ?? "");
  const [biografia, setBiografia] = useState(user?.biografia ?? "");
  const [fotoPerfilUrl, setFotoPerfilUrl] = useState<string | null>(user?.fotoPerfilUrl ?? null);

  const [subiendo, setSubiendo] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  if (!user) return null;

  const cambiarFoto = async () => {
    setSubiendo(true);
    setError(null);
    try {
      const url = await elegirYSubirImagen([1, 1]);
      if (url) setFotoPerfilUrl(url);
    } catch (cause) {
      setError(describeError(cause).message);
    } finally {
      setSubiendo(false);
    }
  };

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      await usuariosApi.actualizarPerfil({
        nombreUsuario: nombreUsuario.trim(),
        nombreCompleto: nombreCompleto.trim(),
        ciudad: ciudad.trim() || null,
        biografia: biografia.trim() || null,
        fotoPerfilUrl,
      });
      await refreshProfile();
      if (router.canGoBack()) router.back();
      else router.replace("/perfil");
    } catch (cause) {
      const { message, fields } = describeError(cause);
      setError(message);
      setFieldErrors(fields);
      setSubmitting(false);
    }
  };

  return (
    <FormScreen eyebrow="Cuenta" title="Datos personales">
      <Pressable onPress={cambiarFoto} disabled={subiendo} style={({ pressed }) => ({ alignSelf: "center", alignItems: "center", gap: theme.spacing.sm, opacity: pressed ? 0.7 : 1 })}>
        <View>
          <Avatar nombre={nombreCompleto || user.nombreCompleto} fotoUrl={fotoPerfilUrl} size={96} />
          <View
            style={{
              position: "absolute",
              right: 0,
              bottom: 0,
              width: 32,
              height: 32,
              borderRadius: 16,
              alignItems: "center",
              justifyContent: "center",
              backgroundColor: theme.colors.accent,
              borderWidth: 2,
              borderColor: theme.colors.background,
            }}
          >
            {subiendo ? <ActivityIndicator size="small" color="#FFFFFF" /> : <Feather name="camera" size={15} color="#FFFFFF" />}
          </View>
        </View>
        <Text variant="captionMedium" color="accent">
          Cambiar foto
        </Text>
      </Pressable>

      <Input label="Nombre de usuario" icon="at-sign" value={nombreUsuario} onChangeText={setNombreUsuario} autoCapitalize="none" autoCorrect={false} maxLength={50} error={fieldErrors.nombreUsuario} />
      <Input label="Nombre completo" icon="user" value={nombreCompleto} onChangeText={setNombreCompleto} maxLength={100} error={fieldErrors.nombreCompleto} />
      <Input label="Ciudad" icon="map-pin" value={ciudad} onChangeText={setCiudad} maxLength={80} error={fieldErrors.ciudad} />
      <Input label="Biografía" placeholder="Qué rutas te gustan, desde cuándo ruedas…" value={biografia} onChangeText={setBiografia} multiline maxLength={280} error={fieldErrors.biografia} />

      <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
        <Feather name="mail" size={14} color={theme.colors.inkFaint} />
        <Text variant="caption" color="inkFaint">
          {user.email}
        </Text>
      </View>

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label="Guardar cambios" size="lg" fullWidth loading={submitting} disabled={!nombreUsuario.trim() || !nombreCompleto.trim() || subiendo} onPress={guardar} />
    </FormScreen>
  );
}
