import * as usuariosApi from "@/api/usuarios";
import { useAuth } from "@/auth/AuthContext";
import { FormScreen } from "@/components/FormScreen";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useState } from "react";
import { View } from "react-native";

const SE_BORRA = [
  "Tu perfil, foto y datos personales",
  "Tus motos, rutas y valoraciones",
  "Tus publicaciones, comentarios y me gusta",
  "Tus amistades, quedadas, mensajes y notificaciones",
];

export default function EliminarCuentaScreen() {
  const theme = useTheme();
  const { user, clearLocalSession } = useAuth();

  const [confirmacion, setConfirmacion] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;

  const confirmado = confirmacion.trim().toLowerCase() === user.nombreUsuario.toLowerCase();

  const eliminar = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await usuariosApi.eliminarMiCuenta();
      // La cuenta ya no existe: no tiene sentido llamar a /logout.
      await clearLocalSession();
    } catch (cause) {
      setError(describeError(cause).message);
      setSubmitting(false);
    }
  };

  return (
    <FormScreen eyebrow="Zona peligrosa" title="Eliminar cuenta" intro="Se borrará de forma permanente e inmediata. No se puede deshacer.">
      <View style={{ backgroundColor: theme.colors.dangerSoft, borderRadius: theme.radius.lg, padding: theme.spacing.lg, gap: theme.spacing.md }}>
        <Text variant="bodyMedium" color="danger">
          Se eliminará todo:
        </Text>
        {SE_BORRA.map((linea) => (
          <View key={linea} style={{ flexDirection: "row", gap: theme.spacing.sm, alignItems: "center" }}>
            <Feather name="x-circle" size={15} color={theme.colors.danger} />
            <Text variant="caption" style={{ flex: 1 }}>
              {linea}
            </Text>
          </View>
        ))}
      </View>

      <Input
        label={`Escribe "${user.nombreUsuario}" para confirmar`}
        value={confirmacion}
        onChangeText={setConfirmacion}
        autoCapitalize="none"
        autoCorrect={false}
      />

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label="Eliminar mi cuenta" variant="danger" size="lg" fullWidth loading={submitting} disabled={!confirmado} onPress={eliminar} />
    </FormScreen>
  );
}
