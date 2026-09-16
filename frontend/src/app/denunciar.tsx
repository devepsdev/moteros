import * as denunciasApi from "@/api/denuncias";
import { MOTIVOS_DENUNCIA, type MotivoDenuncia, type TipoDenuncia } from "@/api/denuncias";
import { FormScreen } from "@/components/FormScreen";
import { OptionPicker } from "@/components/OptionPicker";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Text } from "@/components/ui/Text";
import { confirmarBloqueo } from "@/lib/bloqueo";
import { describeError } from "@/lib/errors";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { Alert } from "react-native";

const QUE: Record<TipoDenuncia, string> = {
  usuario: "este perfil",
  publicacion: "esta publicación",
  comentario: "este comentario",
  mensaje: "este mensaje",
  ruta: "esta ruta",
  quedada: "esta quedada",
};

/** Denuncia de un contenido. Parámetros: tipo, uuid y, si se conoce, el autor (para ofrecer bloquearlo). */
export default function DenunciarScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ tipo: TipoDenuncia; uuid: string; autorUuid?: string; autorNombre?: string }>();
  const [motivo, setMotivo] = useState<MotivoDenuncia | undefined>();
  const [descripcion, setDescripcion] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/"));

  const enviar = async () => {
    if (!motivo) return;
    setEnviando(true);
    setError(null);
    try {
      await denunciasApi.denunciar({ tipo: params.tipo, referenciaUuid: params.uuid, motivo, descripcion: descripcion.trim() || undefined });
      const autor = params.autorUuid && params.autorNombre ? { uuid: params.autorUuid, nombre: params.autorNombre } : null;
      Alert.alert("Denuncia enviada", "Gracias por avisar. La revisaremos en un plazo máximo de 24 horas.", [
        ...(autor
          ? [{ text: `Bloquear a ${autor.nombre}`, onPress: () => confirmarBloqueo(autor, () => router.dismissTo("/")) }]
          : []),
        { text: "Hecho", onPress: volver },
      ]);
    } catch (cause) {
      setError(describeError(cause).message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <FormScreen eyebrow="Denunciar" title={`¿Qué pasa con ${QUE[params.tipo] ?? "este contenido"}?`} intro="Tu denuncia es anónima: el autor no sabrá quién la ha enviado." fallbackHref="/">
      <OptionPicker label="Motivo" options={MOTIVOS_DENUNCIA} value={motivo} onChange={setMotivo} />

      <Input
        label="Detalles (opcional)"
        placeholder="Cuéntanos lo que haya que saber"
        value={descripcion}
        onChangeText={setDescripcion}
        multiline
        maxLength={1000}
      />

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label="Enviar denuncia" size="lg" fullWidth loading={enviando} disabled={!motivo} onPress={enviar} />
    </FormScreen>
  );
}
