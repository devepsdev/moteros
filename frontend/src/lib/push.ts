import * as dispositivosApi from "@/api/dispositivos";
import Constants from "expo-constants";
import * as Device from "expo-device";
import * as Notifications from "expo-notifications";
import { useRouter } from "expo-router";
import { useEffect } from "react";
import { Platform } from "react-native";

/** Con la app abierta, el aviso se muestra igualmente: si no, parece que no llega nada. */
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

let tokenActual: string | null = null;

/** El identificador del proyecto de EAS, que es lo que necesita Expo para emitir el token. */
function projectId(): string | undefined {
  return (
    Constants.expoConfig?.extra?.eas?.projectId ??
    (Constants.easConfig as { projectId?: string } | undefined)?.projectId
  );
}

/**
 * Pide permiso y devuelve el token de este móvil, o null si no se puede (emulador, permiso
 * denegado o app de desarrollo sin proyecto).
 */
async function obtenerToken(): Promise<string | null> {
  if (!Device.isDevice) return null;
  try {
    if (Platform.OS === "android") {
      // Sin canal, Android no enseña nada en la barra de notificaciones.
      await Notifications.setNotificationChannelAsync("default", {
        name: "Avisos",
        importance: Notifications.AndroidImportance.DEFAULT,
        lightColor: "#FF6A13",
      });
    }
    const { status } = await Notifications.getPermissionsAsync();
    const concedido =
      status === "granted" || (await Notifications.requestPermissionsAsync()).status === "granted";
    if (!concedido) return null;
    const id = projectId();
    const { data } = await Notifications.getExpoPushTokenAsync(id ? { projectId: id } : undefined);
    return data;
  } catch {
    return null;
  }
}

/**
 * Registra este móvil en el servidor mientras haya sesión y lleva al usuario a la pantalla que
 * toca cuando toca un aviso. Se usa una sola vez, en el layout raíz.
 */
export function useAvisosPush(activo: boolean) {
  const router = useRouter();

  useEffect(() => {
    if (!activo) return;
    let vivo = true;
    (async () => {
      const token = await obtenerToken();
      if (!token || !vivo) return;
      tokenActual = token;
      await dispositivosApi.registrar(token, Platform.OS).catch(() => {});
    })();
    return () => {
      vivo = false;
    };
  }, [activo]);

  useEffect(() => {
    if (!activo) return;
    const sub = Notifications.addNotificationResponseReceivedListener((respuesta) => {
      const datos = respuesta.notification.request.content.data as { tipo?: string } | undefined;
      // El chat abre la lista de conversaciones; el resto, las notificaciones.
      router.push(datos?.tipo === "mensaje" ? "/chat" : "/notificaciones");
    });
    return () => sub.remove();
  }, [activo, router]);
}

/** Da de baja este móvil al cerrar sesión, para que no le sigan llegando avisos de esa cuenta. */
export async function darDeBajaEsteMovil(): Promise<void> {
  if (!tokenActual) return;
  await dispositivosApi.eliminar(tokenActual).catch(() => {});
  tokenActual = null;
}
