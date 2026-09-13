import { IconButton } from "@/components/ui/IconButton";
import { Screen } from "@/components/ui/Screen";
import { Text } from "@/components/ui/Text";
import { useTheme } from "@/theme";
import { useRouter, type Href } from "expo-router";
import type { ReactNode } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, View } from "react-native";

/**
 * Esqueleto común de las pantallas de formulario: volver, antetítulo, título,
 * entradilla y contenido desplazable que no queda bajo el teclado.
 */
export function FormScreen({
  eyebrow,
  title,
  intro,
  fallbackHref = "/perfil",
  children,
}: {
  eyebrow: string;
  title: string;
  intro?: string;
  /** A dónde volver si la pantalla se abrió sin historial. */
  fallbackHref?: Href;
  children: ReactNode;
}) {
  const theme = useTheme();
  const router = useRouter();

  return (
    <Screen>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={{ paddingBottom: theme.spacing.huge }}
        >
          <View style={{ paddingHorizontal: theme.screenPadding }}>
            <IconButton
              name="arrow-left"
              accessibilityLabel="Volver"
              onPress={() => (router.canGoBack() ? router.back() : router.replace(fallbackHref))}
              style={{ marginLeft: -10 }}
            />
          </View>

          <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.sm, marginTop: theme.spacing.md }}>
            <Text variant="overline" color="accent">
              {eyebrow}
            </Text>
            <Text variant="display">{title}</Text>
            {intro ? (
              <Text variant="body" color="inkMuted">
                {intro}
              </Text>
            ) : null}
          </View>

          <View style={{ paddingHorizontal: theme.screenPadding, gap: theme.spacing.lg, marginTop: theme.spacing.xxl }}>
            {children}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
