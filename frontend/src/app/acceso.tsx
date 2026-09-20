import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { abrirLegal } from "@/lib/legal";
import { useTheme } from "@/theme";
import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import Feather from "@expo/vector-icons/Feather";
import MaterialCommunityIcons from "@expo/vector-icons/MaterialCommunityIcons";
import { useRouter } from "expo-router";
import { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, View } from "react-native";

type Mode = "login" | "registro";

export default function AccesoScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { login, registro } = useAuth();

  const [mode, setMode] = useState<Mode>("login");
  const [identificador, setIdentificador] = useState("");
  const [nombreUsuario, setNombreUsuario] = useState("");
  const [nombreCompleto, setNombreCompleto] = useState("");
  const [email, setEmail] = useState("");
  const [ciudad, setCiudad] = useState("");
  const [password, setPassword] = useState("");
  const [aceptaTerminos, setAceptaTerminos] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const isLogin = mode === "login";

  const switchMode = () => {
    setMode(isLogin ? "registro" : "login");
    setError(null);
    setFieldErrors({});
  };

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      if (isLogin) {
        await login({ identificador: identificador.trim(), password });
      } else {
        await registro({
          nombreUsuario: nombreUsuario.trim(),
          nombreCompleto: nombreCompleto.trim(),
          email: email.trim(),
          password,
          ciudad: ciudad.trim() || undefined,
          aceptaTerminos,
        });
      }
      // Stack.Protected lleva a las pestañas al cambiar la sesión.
    } catch (cause) {
      const { message, fields } = describeError(cause);
      setError(message);
      setFieldErrors(fields);
    } finally {
      setSubmitting(false);
    }
  };

  const canSubmit = isLogin
    ? identificador.trim().length > 0 && password.length > 0
    : nombreUsuario.trim().length >= 3 && nombreCompleto.trim().length > 0 && email.trim().length > 0 && password.length >= 8 && aceptaTerminos;

  return (
    <Screen sinDegradado>
      {/* Carretera de montaña de fondo. El velo la deja a la vista arriba y ya es opaco al
          llegar al formulario, que así se lee sobre fondo liso. */}
      <Image
        source={require("../../assets/images/acceso-carretera.webp")}
        style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0 }}
        contentFit="cover"
        transition={300}
      />
      <LinearGradient
        pointerEvents="none"
        colors={
          theme.isDark
            ? ["rgba(14, 15, 17, 0.30)", "rgba(14, 15, 17, 0.62)", theme.colors.background, theme.colors.background]
            : ["rgba(245, 244, 241, 0.38)", "rgba(245, 244, 241, 0.70)", theme.colors.background, theme.colors.background]
        }
        locations={[0, 0.16, 0.34, 1]}
        style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0 }}
      />
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={{ flexGrow: 1, paddingHorizontal: theme.screenPadding, paddingBottom: theme.spacing.huge }}
        >
          <View style={{ marginTop: theme.spacing.huge, gap: theme.spacing.sm }}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: theme.spacing.sm }}>
              <MaterialCommunityIcons name="motorbike" size={34} color={theme.colors.accent} />
              <Text variant="display" style={{ fontSize: 34 }}>
                moter<Text variant="display" color="accent" style={{ fontSize: 34 }}>@</Text>s
              </Text>
            </View>
            <Text variant="title1" style={{ marginTop: theme.spacing.xl }}>
              {isLogin ? "Arranca" : "Únete a la ruta"}
            </Text>
            <Text variant="body" color="inkMuted">
              {isLogin
                ? "Entra con tu email o tu nombre de usuario."
                : "Comparte tus rutas, organiza quedadas y conoce a otros moteros."}
            </Text>
          </View>

          <View style={{ gap: theme.spacing.lg, marginTop: theme.spacing.xxxl }}>
            {isLogin ? (
              <Input
                label="Email o usuario"
                icon="user"
                placeholder="tucorreo@ejemplo.com"
                value={identificador}
                onChangeText={setIdentificador}
                autoCapitalize="none"
                autoCorrect={false}
                error={fieldErrors.identificador}
              />
            ) : (
              <>
                <Input
                  label="Nombre de usuario"
                  icon="at-sign"
                  placeholder="lobo_asfalto"
                  value={nombreUsuario}
                  onChangeText={setNombreUsuario}
                  autoCapitalize="none"
                  autoCorrect={false}
                  error={fieldErrors.nombreUsuario}
                />
                <Input
                  label="Nombre completo"
                  icon="user"
                  placeholder="Tu nombre"
                  value={nombreCompleto}
                  onChangeText={setNombreCompleto}
                  autoCapitalize="words"
                  error={fieldErrors.nombreCompleto}
                />
                <Input
                  label="Email"
                  icon="mail"
                  placeholder="tucorreo@ejemplo.com"
                  value={email}
                  onChangeText={setEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                  error={fieldErrors.email}
                />
                <Input
                  label="Ciudad (opcional)"
                  icon="map-pin"
                  placeholder="Barcelona"
                  value={ciudad}
                  onChangeText={setCiudad}
                  error={fieldErrors.ciudad}
                />
              </>
            )}

            <Input
              label="Contraseña"
              icon="lock"
              placeholder={isLogin ? "Tu contraseña" : "Mínimo 8 caracteres"}
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoCapitalize="none"
              error={fieldErrors.password}
            />

            {isLogin ? (
              <Pressable onPress={() => router.push("/recuperar")} style={({ pressed }) => ({ alignSelf: "flex-end", opacity: pressed ? 0.6 : 1 })}>
                <Text variant="captionMedium" color="accent">
                  ¿Has olvidado tu contraseña?
                </Text>
              </Pressable>
            ) : (
              <Pressable
                onPress={() => setAceptaTerminos((v) => !v)}
                accessibilityRole="checkbox"
                accessibilityState={{ checked: aceptaTerminos }}
                style={{ flexDirection: "row", alignItems: "flex-start", gap: theme.spacing.md }}
              >
                <Feather name={aceptaTerminos ? "check-square" : "square"} size={22} color={aceptaTerminos ? theme.colors.accent : theme.colors.inkMuted} />
                <Text variant="caption" color="inkMuted" style={{ flex: 1 }}>
                  Tengo al menos 16 años y acepto los{" "}
                  <Text variant="captionMedium" color="accent" onPress={() => abrirLegal("terminos")}>
                    términos de uso
                  </Text>{" "}
                  y la{" "}
                  <Text variant="captionMedium" color="accent" onPress={() => abrirLegal("privacidad")}>
                    política de privacidad
                  </Text>
                  . No se tolera el contenido ofensivo ni los usuarios abusivos.
                </Text>
              </Pressable>
            )}

            {error ? (
              <Text variant="caption" color="danger">
                {error}
              </Text>
            ) : null}

            <Button
              label={isLogin ? "Entrar" : "Crear cuenta"}
              size="lg"
              fullWidth
              loading={submitting}
              disabled={!canSubmit}
              onPress={submit}
              style={{ marginTop: theme.spacing.sm }}
            />
          </View>

          <Pressable onPress={switchMode} style={({ pressed }) => ({ marginTop: theme.spacing.xxl, alignItems: "center", opacity: pressed ? 0.6 : 1 })}>
            <Text variant="caption" color="inkMuted">
              {isLogin ? "¿Todavía no tienes cuenta? " : "¿Ya tienes cuenta? "}
              <Text variant="captionMedium" color="accent">
                {isLogin ? "Regístrate" : "Inicia sesión"}
              </Text>
            </Text>
          </Pressable>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
