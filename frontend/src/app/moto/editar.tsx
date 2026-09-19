import { mediaUrl } from "@/api/config";
import * as motosApi from "@/api/motos";
import { FormScreen } from "@/components/FormScreen";
import { OptionPicker } from "@/components/OptionPicker";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { LoadingView } from "@/components/ui/ListFooter";
import { Text } from "@/components/ui/Text";
import { describeError } from "@/lib/errors";
import { TIPOS_MOTO } from "@/lib/format";
import { elegirYSubirImagen } from "@/lib/pickImage";
import { useTheme } from "@/theme";
import type { MotoResponse, TipoMoto } from "@/types/dto";
import MaterialCommunityIcons from "@expo/vector-icons/MaterialCommunityIcons";
import { Image } from "expo-image";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, Alert, Pressable, View } from "react-native";

/** Alta (sin parámetro) o edición (`?uuid=`) de una moto del garaje. */
export default function EditarMotoScreen() {
  const { uuid } = useLocalSearchParams<{ uuid?: string }>();
  const [moto, setMoto] = useState<MotoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!uuid) return;
    motosApi.obtener(uuid).then(setMoto, (cause) => setError(describeError(cause).message));
  }, [uuid]);

  if (uuid && !moto) {
    return (
      <FormScreen eyebrow="Garaje" title="Editar moto">
        {error ? (
          <Text variant="body" color="danger">
            {error}
          </Text>
        ) : (
          <LoadingView />
        )}
      </FormScreen>
    );
  }

  return <MotoForm key={moto?.uuid ?? "nueva"} moto={moto} />;
}

function MotoForm({ moto }: { moto: MotoResponse | null }) {
  const theme = useTheme();
  const router = useRouter();

  const [marca, setMarca] = useState(moto?.marca ?? "");
  const [modelo, setModelo] = useState(moto?.modelo ?? "");
  const [anio, setAnio] = useState(moto?.anio ? String(moto.anio) : "");
  const [cilindrada, setCilindrada] = useState(moto?.cilindradaCc ? String(moto.cilindradaCc) : "");
  const [tipo, setTipo] = useState<TipoMoto>(moto?.tipo ?? "naked");
  const [fotoUrl, setFotoUrl] = useState<string | null>(moto?.fotoUrl ?? null);

  const [subiendo, setSubiendo] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const volver = () => (router.canGoBack() ? router.back() : router.replace("/perfil"));

  const elegirFoto = async () => {
    setSubiendo(true);
    setError(null);
    try {
      const url = await elegirYSubirImagen([16, 10]);
      if (url) setFotoUrl(url);
    } catch (cause) {
      Alert.alert("No se ha podido subir la foto", describeError(cause).message);
    } finally {
      setSubiendo(false);
    }
  };

  const guardar = async () => {
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    const data = {
      marca: marca.trim(),
      modelo: modelo.trim(),
      anio: anio ? Number(anio) : null,
      cilindradaCc: cilindrada ? Number(cilindrada) : null,
      tipo,
      fotoUrl,
    };
    try {
      if (moto) await motosApi.actualizar(moto.uuid, data);
      else await motosApi.crear(data);
      volver();
    } catch (cause) {
      const { message, fields } = describeError(cause);
      setError(message);
      setFieldErrors(fields);
      setSubmitting(false);
    }
  };

  const eliminar = () =>
    moto &&
    Alert.alert("Eliminar moto", `¿Quitar la ${moto.marca} ${moto.modelo} de tu garaje?`, [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: async () => {
          try {
            await motosApi.eliminar(moto.uuid);
            volver();
          } catch (cause) {
            setError(describeError(cause).message);
          }
        },
      },
    ]);

  const preview = mediaUrl(fotoUrl);

  return (
    <FormScreen eyebrow="Garaje" title={moto ? "Editar moto" : "Nueva moto"}>
      <Pressable
        onPress={elegirFoto}
        disabled={subiendo}
        style={({ pressed }) => ({
          aspectRatio: 16 / 10,
          borderRadius: theme.radius.lg,
          overflow: "hidden",
          backgroundColor: theme.colors.surfaceSunken,
          borderWidth: preview ? 0 : 1,
          borderStyle: "dashed",
          borderColor: theme.colors.borderStrong,
          alignItems: "center",
          justifyContent: "center",
          opacity: pressed ? 0.8 : 1,
        })}
      >
        {preview ? (
          <>
            <Image source={{ uri: preview }} style={{ width: "100%", height: "100%" }} contentFit="cover" />
            <IconButton name="x" variant="floating" accessibilityLabel="Quitar foto" onPress={() => setFotoUrl(null)} style={{ position: "absolute", top: theme.spacing.sm, right: theme.spacing.sm }} />
          </>
        ) : subiendo ? (
          <ActivityIndicator color={theme.colors.accent} />
        ) : (
          <View style={{ alignItems: "center", gap: theme.spacing.sm }}>
            <MaterialCommunityIcons name="camera-plus-outline" size={30} color={theme.colors.inkMuted} />
            <Text variant="captionMedium" color="inkMuted">
              Añadir foto
            </Text>
          </View>
        )}
      </Pressable>

      <View style={{ flexDirection: "row", gap: theme.spacing.md }}>
        <Input label="Marca" placeholder="Yamaha" value={marca} onChangeText={setMarca} maxLength={50} error={fieldErrors.marca} containerStyle={{ flex: 1 }} />
        <Input label="Modelo" placeholder="MT-07" value={modelo} onChangeText={setModelo} maxLength={60} error={fieldErrors.modelo} containerStyle={{ flex: 1 }} />
      </View>
      <View style={{ flexDirection: "row", gap: theme.spacing.md }}>
        <Input label="Año" placeholder="2021" value={anio} onChangeText={(t) => setAnio(t.replace(/\D/g, ""))} keyboardType="number-pad" maxLength={4} error={fieldErrors.anio} containerStyle={{ flex: 1 }} />
        <Input label="Cilindrada (cc)" placeholder="689" value={cilindrada} onChangeText={(t) => setCilindrada(t.replace(/\D/g, ""))} keyboardType="number-pad" maxLength={5} error={fieldErrors.cilindradaCc} containerStyle={{ flex: 1 }} />
      </View>
      <OptionPicker label="Tipo" options={TIPOS_MOTO} value={tipo} onChange={setTipo} />

      {error ? (
        <Text variant="caption" color="danger">
          {error}
        </Text>
      ) : null}

      <Button label={moto ? "Guardar cambios" : "Añadir al garaje"} size="lg" fullWidth loading={submitting} disabled={!marca.trim() || !modelo.trim() || subiendo} onPress={guardar} />
      {moto ? <Button label="Eliminar moto" variant="ghost" fullWidth onPress={eliminar} /> : null}
    </FormScreen>
  );
}
