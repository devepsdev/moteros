import * as authApi from "@/api/auth";
import { useAuth } from "@/auth/AuthContext";
import { FormScreen } from "@/components/FormScreen";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { useState } from "react";
import { Alert } from "react-native";

export default function ContrasenaScreen() {
  const { clearLocalSession } = useAuth();

  const [actual, setActual] = useState("");
  const [nueva, setNueva] = useState("");
  const [repetir, setRepetir] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const noCoinciden = repetir.length > 0 && nueva !== repetir;

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await authApi.cambiarPassword({ passwordActual: actual, passwordNueva: nueva });
      // El backend revoca todas las sesiones, también esta: hay que volver a entrar.
      Alert.alert("Contraseña cambiada", "Por seguridad se han cerrado todas tus sesiones. Entra de nuevo con la contraseña nueva.", [
        { text: "Entendido", onPress: () => clearLocalSession() },
      ]);
    } catch (cause) {
      setError(describeError(cause).message);
      setSubmitting(false);
    }
  };

  return (
    <FormScreen eyebrow="Cuenta" title="Cambiar contraseña" intro="Al cambiarla se cerrará la sesión en todos tus dispositivos.">
      <Input label="Contraseña actual" icon="lock" value={actual} onChangeText={setActual} secureTextEntry autoCapitalize="none" />
      <Input label="Nueva contraseña" icon="key" placeholder="Mínimo 8 caracteres" value={nueva} onChangeText={setNueva} secureTextEntry autoCapitalize="none" maxLength={72} />
      <Input
        label="Repite la nueva contraseña"
        icon="key"
        value={repetir}
        onChangeText={setRepetir}
        secureTextEntry
        autoCapitalize="none"
        maxLength={72}
        error={noCoinciden ? "Las contraseñas no coinciden" : undefined}
      />

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button
        label="Cambiar contraseña"
        size="lg"
        fullWidth
        loading={submitting}
        disabled={!actual || nueva.length < 8 || nueva !== repetir}
        onPress={guardar}
      />
    </FormScreen>
  );
}
