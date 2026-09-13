import * as authApi from "@/api/auth";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, TextInput, View } from "react-native";

type Step = "solicitar" | "verificar" | "listo";

export default function RecuperarScreen() {
  const theme = useTheme();
  const router = useRouter();

  const [step, setStep] = useState<Step>("solicitar");
  const [email, setEmail] = useState("");
  const [codigo, setCodigo] = useState("");
  const [passwordNueva, setPasswordNueva] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const irALogin = () => (router.canGoBack() ? router.back() : router.replace("/acceso"));

  const pedirCodigo = async (reenviando = false) => {
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      await authApi.recuperarPassword(email.trim());
      setStep("verificar");
      if (reenviando) setNotice("Te hemos enviado un código nuevo.");
    } catch (cause) {
      setError(describeError(cause).message);
    } finally {
      setSubmitting(false);
    }
  };

  const restablecer = async () => {
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      await authApi.restablecerPassword({ email: email.trim(), codigo, passwordNueva });
      setStep("listo");
    } catch (cause) {
      setError(describeError(cause).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={{ flexGrow: 1, paddingHorizontal: theme.screenPadding, paddingBottom: theme.spacing.huge }}
        >
          {step !== "listo" ? (
            <IconButton
              name="arrow-left"
              accessibilityLabel="Volver"
              style={{ marginLeft: -10 }}
              onPress={() => {
                if (step === "verificar") {
                  setStep("solicitar");
                  setError(null);
                  setNotice(null);
                } else {
                  irALogin();
                }
              }}
            />
          ) : null}

          {step === "solicitar" ? (
            <>
              <Header eyebrow="Paso 1 de 2" title="Recupera tu cuenta" body="Escribe tu email y te enviaremos un código de 6 dígitos para crear una contraseña nueva." />
              <View style={{ gap: theme.spacing.lg, marginTop: theme.spacing.xxxl }}>
                <Input
                  label="Email"
                  icon="mail"
                  placeholder="tucorreo@ejemplo.com"
                  value={email}
                  onChangeText={setEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                />
                {error ? <Text variant="caption" color="danger">{error}</Text> : null}
                <Button label="Enviar código" size="lg" fullWidth loading={submitting} disabled={email.trim().length === 0} onPress={() => pedirCodigo()} />
              </View>
            </>
          ) : step === "verificar" ? (
            <>
              <Header eyebrow="Paso 2 de 2" title="Revisa tu correo" body={`Si ${email.trim()} está registrado, te habrá llegado un código de 6 dígitos. Caduca en 15 minutos.`} />
              <View style={{ gap: theme.spacing.lg, marginTop: theme.spacing.xxxl }}>
                <View style={{ gap: theme.spacing.sm }}>
                  <Text variant="overline" color="inkFaint">
                    Código
                  </Text>
                  <CodeInput value={codigo} onChangeText={setCodigo} />
                </View>
                <Input
                  label="Nueva contraseña"
                  icon="lock"
                  placeholder="Mínimo 8 caracteres"
                  value={passwordNueva}
                  onChangeText={setPasswordNueva}
                  secureTextEntry
                  autoCapitalize="none"
                />
                {error ? <Text variant="caption" color="danger">{error}</Text> : null}
                {notice ? <Text variant="caption" color="support">{notice}</Text> : null}
                <Button
                  label="Cambiar contraseña"
                  size="lg"
                  fullWidth
                  loading={submitting}
                  disabled={codigo.length < 6 || passwordNueva.length < 8}
                  onPress={restablecer}
                />
                <Pressable
                  onPress={submitting ? undefined : () => pedirCodigo(true)}
                  style={({ pressed }) => ({ alignItems: "center", paddingVertical: theme.spacing.sm, opacity: pressed ? 0.6 : 1 })}
                >
                  <Text variant="caption" color="inkMuted">
                    ¿No te ha llegado?{" "}
                    <Text variant="captionMedium" color="accent">
                      Reenviar código
                    </Text>
                  </Text>
                </Pressable>
              </View>
            </>
          ) : (
            <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: theme.spacing.lg }}>
              <View style={{ width: 72, height: 72, borderRadius: theme.radius.full, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.supportSoft }}>
                <Feather name="check" size={30} color={theme.colors.support} />
              </View>
              <Text variant="title1" center>
                Contraseña actualizada
              </Text>
              <Text variant="body" color="inkMuted" center style={{ maxWidth: 300 }}>
                Ya puedes entrar con tu nueva contraseña. Se ha cerrado la sesión en el resto de dispositivos.
              </Text>
              <Button label="Iniciar sesión" size="lg" onPress={irALogin} style={{ marginTop: theme.spacing.sm }} />
            </View>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

function Header({ eyebrow, title, body }: { eyebrow: string; title: string; body: string }) {
  const theme = useTheme();
  return (
    <View style={{ gap: theme.spacing.sm, marginTop: theme.spacing.xl }}>
      <Text variant="overline" color="accent">
        {eyebrow}
      </Text>
      <Text variant="display">{title}</Text>
      <Text variant="body" color="inkMuted">
        {body}
      </Text>
    </View>
  );
}

function CodeInput({ value, onChangeText }: { value: string; onChangeText: (value: string) => void }) {
  const theme = useTheme();
  const [focused, setFocused] = useState(false);
  return (
    <TextInput
      value={value}
      onChangeText={(text) => onChangeText(text.replace(/\D/g, "").slice(0, 6))}
      keyboardType="number-pad"
      maxLength={6}
      placeholder="000000"
      placeholderTextColor={theme.colors.inkFaint}
      selectionColor={theme.colors.accent}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      style={{
        height: 68,
        borderRadius: theme.radius.md,
        borderWidth: 1,
        borderColor: focused ? theme.colors.accent : theme.colors.border,
        backgroundColor: theme.colors.surfaceSunken,
        color: theme.colors.ink,
        fontFamily: theme.fontFamily.display,
        fontSize: 32,
        textAlign: "center",
        letterSpacing: 12,
        paddingLeft: 12,
      }}
    />
  );
}
