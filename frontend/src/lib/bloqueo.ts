import * as bloqueosApi from "@/api/bloqueos";
import { describeError } from "@/lib/errors";
import { Alert } from "react-native";

/** Pide confirmación, bloquea al usuario y llama a `onHecho` si ha ido bien. */
export function confirmarBloqueo(usuario: { uuid: string; nombre: string }, onHecho: () => void) {
  Alert.alert(
    `Bloquear a ${usuario.nombre}`,
    "No podréis escribiros ni ser amigos, y dejaréis de ver las publicaciones y comentarios del otro. No se le avisa. Puedes desbloquearlo desde tu perfil.",
    [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Bloquear",
        style: "destructive",
        onPress: () =>
          bloqueosApi
            .bloquear(usuario.uuid)
            .then(onHecho)
            .catch((cause) => Alert.alert("No se ha podido bloquear", describeError(cause).message)),
      },
    ]
  );
}
