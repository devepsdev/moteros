import * as rutasApi from "@/api/rutas";
import { FormScreen } from "@/components/FormScreen";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { StarPicker } from "@/components/ui/Stars";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { useTheme } from "@/theme";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert, View } from "react-native";

const ETIQUETAS = ["", "Mejor evitarla", "Regular", "Está bien", "Muy buena", "Imprescindible"];

export default function ValorarScreen() {
  const theme = useTheme();
  const router = useRouter();
  const params = useLocalSearchParams<{ uuid: string; nombre?: string; puntuacion?: string; comentario?: string }>();
  const editando = Boolean(params.puntuacion);

  const [puntuacion, setPuntuacion] = useState(params.puntuacion ? Number(params.puntuacion) : 0);
  const [comentario, setComentario] = useState(params.comentario ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cerrar = () => (router.canGoBack() ? router.back() : router.replace({ pathname: "/ruta/[uuid]", params: { uuid: params.uuid } }));

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await rutasApi.valorar(params.uuid, { puntuacion, comentario: comentario.trim() || null });
      cerrar();
    } catch (cause) {
      setError(describeError(cause).message);
      setSubmitting(false);
    }
  };

  const eliminar = () =>
    Alert.alert("Eliminar valoración", "¿Quieres borrar tu valoración de esta ruta?", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: async () => {
          try {
            await rutasApi.eliminarMiValoracion(params.uuid);
            cerrar();
          } catch (cause) {
            setError(describeError(cause).message);
          }
        },
      },
    ]);

  return (
    <FormScreen eyebrow={params.nombre ?? "Ruta"} title={editando ? "Tu valoración" : "¿Qué tal la ruta?"} intro="Ayuda a otros moteros a decidir si merece la pena.">
      <View style={{ alignItems: "center", gap: theme.spacing.md, paddingVertical: theme.spacing.lg }}>
        <StarPicker value={puntuacion} onChange={setPuntuacion} />
        <Text variant="title3" color={puntuacion ? "amber" : "inkFaint"}>
          {puntuacion ? ETIQUETAS[puntuacion] : "Toca para puntuar"}
        </Text>
      </View>

      <Input
        label="Comentario (opcional)"
        placeholder="Estado del asfalto, tráfico, mejores paradas…"
        value={comentario}
        onChangeText={setComentario}
        multiline
        maxLength={280}
      />
      <Text variant="caption" color="inkFaint" style={{ alignSelf: "flex-end", marginTop: -theme.spacing.sm }}>
        {comentario.length}/280
      </Text>

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label={editando ? "Guardar cambios" : "Publicar valoración"} size="lg" fullWidth loading={submitting} disabled={puntuacion === 0} onPress={guardar} />
      {editando ? <Button label="Eliminar mi valoración" variant="ghost" fullWidth onPress={eliminar} /> : null}
    </FormScreen>
  );
}
