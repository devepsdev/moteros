import * as amistadesApi from "@/api/amistades";
import { Button } from "@/components/ui/Button";
import { describeError } from "@/lib/errors";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import type { UsuarioResponse } from "@/types/dto";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Alert, View } from "react-native";

/** Botones de amistad y mensaje en el perfil de otro usuario, según la relación actual. */
export function AmistadAcciones({ usuario, onCambio }: { usuario: UsuarioResponse; onCambio?: () => void }) {
  const theme = useTheme();
  const router = useRouter();
  const relacion = useAsync(() => amistadesApi.relacionCon(usuario.uuid), [usuario.uuid]);
  const [ocupado, setOcupado] = useState(false);

  const ejecutar = async (fn: () => Promise<unknown>) => {
    setOcupado(true);
    try {
      await fn();
      relacion.reload();
      onCambio?.();
    } catch (cause) {
      Alert.alert("No se ha podido completar", describeError(cause).message);
    } finally {
      setOcupado(false);
    }
  };

  const confirmar = (titulo: string, mensaje: string, boton: string, fn: () => Promise<unknown>) =>
    Alert.alert(titulo, mensaje, [
      { text: "Volver", style: "cancel" },
      { text: boton, style: "destructive", onPress: () => ejecutar(fn) },
    ]);

  const mensaje = (
    <Button
      label="Mensaje"
      variant="secondary"
      style={{ flex: 1 }}
      icon={<Feather name="message-circle" size={16} color={theme.colors.ink} />}
      onPress={() =>
        router.push({ pathname: "/chat/usuario/[usuarioUuid]", params: { usuarioUuid: usuario.uuid, nombre: usuario.nombreCompleto, foto: usuario.fotoPerfilUrl ?? undefined } })
      }
    />
  );

  const a = relacion.data;
  let amistad;
  if (relacion.loading && !a) {
    amistad = <Button label="…" variant="secondary" disabled style={{ flex: 1 }} />;
  } else if (a?.estado === "aceptada") {
    amistad = (
      <Button
        label="Amigos"
        variant="secondary"
        style={{ flex: 1 }}
        loading={ocupado}
        icon={<Feather name="user-check" size={16} color={theme.colors.support} />}
        onPress={() => confirmar("Dejar de ser amigos", `¿Quitar a ${usuario.nombreCompleto} de tus amigos?`, "Quitar", () => amistadesApi.eliminar(usuario.uuid))}
      />
    );
  } else if (a?.estado === "pendiente" && a.enviadaPorMi) {
    amistad = (
      <Button
        label="Solicitud enviada"
        variant="secondary"
        style={{ flex: 1 }}
        loading={ocupado}
        icon={<Feather name="clock" size={16} color={theme.colors.inkMuted} />}
        onPress={() => confirmar("Retirar solicitud", "¿Retirar la solicitud de amistad?", "Retirar", () => amistadesApi.eliminar(usuario.uuid))}
      />
    );
  } else if (a?.estado === "pendiente") {
    return (
      <View style={{ gap: theme.spacing.sm }}>
        <View style={{ flexDirection: "row", gap: theme.spacing.sm }}>
          <Button label="Aceptar solicitud" style={{ flex: 1 }} loading={ocupado} onPress={() => ejecutar(() => amistadesApi.responder(a.uuid, true))} />
          <Button label="Rechazar" variant="secondary" disabled={ocupado} onPress={() => ejecutar(() => amistadesApi.responder(a.uuid, false))} />
        </View>
        {mensaje}
      </View>
    );
  } else {
    amistad = (
      <Button
        label="Añadir amigo"
        style={{ flex: 1 }}
        loading={ocupado}
        icon={<Feather name="user-plus" size={16} color="#FFFFFF" />}
        onPress={() => ejecutar(() => amistadesApi.enviarSolicitud(usuario.uuid))}
      />
    );
  }

  return (
    <View style={{ flexDirection: "row", gap: theme.spacing.sm }}>
      {amistad}
      {mensaje}
    </View>
  );
}
