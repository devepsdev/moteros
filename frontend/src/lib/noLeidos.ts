import * as chatApi from "@/api/chat";
import * as notificacionesApi from "@/api/notificaciones";
import { useEffect, useSyncExternalStore } from "react";
import { AppState } from "react-native";

/**
 * Contadores globales de mensajes y notificaciones sin leer (insignias de la pestaña Chat y de
 * la campana). No hay push ni WebSocket: se consultan periódicamente mientras la app está en
 * primer plano y se refrescan a mano al abrir una conversación o las notificaciones.
 */
interface Contadores {
  mensajes: number;
  notificaciones: number;
}

let estado: Contadores = { mensajes: 0, notificaciones: 0 };
const listeners = new Set<() => void>();

function actualizar(parcial: Partial<Contadores>) {
  const siguiente = { ...estado, ...parcial };
  if (siguiente.mensajes === estado.mensajes && siguiente.notificaciones === estado.notificaciones) return;
  estado = siguiente;
  listeners.forEach((l) => l());
}

function suscribir(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export async function refrescarNoLeidos(): Promise<void> {
  // Sin conexión o sesión caducada: se reintenta en el siguiente ciclo.
  const [mensajes, notificaciones] = await Promise.allSettled([chatApi.totalNoLeidos(), notificacionesApi.contarNoLeidas()]);
  actualizar({
    ...(mensajes.status === "fulfilled" ? { mensajes: mensajes.value } : {}),
    ...(notificaciones.status === "fulfilled" ? { notificaciones: notificaciones.value } : {}),
  });
}

/** Pone el contador de notificaciones a cero sin esperar al servidor (tras "marcar todas"). */
export function vaciarNotificaciones() {
  actualizar({ notificaciones: 0 });
}

export function useNoLeidos(): number {
  return useSyncExternalStore(suscribir, () => estado.mensajes);
}

export function useNotificacionesNoLeidas(): number {
  return useSyncExternalStore(suscribir, () => estado.notificaciones);
}

const INTERVALO_MS = 30_000;

/** Arranca el sondeo de los contadores. Se monta una sola vez, en el layout de pestañas. */
export function useSondeoNoLeidos() {
  useEffect(() => {
    refrescarNoLeidos();
    let id: ReturnType<typeof setInterval> | null = setInterval(refrescarNoLeidos, INTERVALO_MS);

    const sub = AppState.addEventListener("change", (appState) => {
      if (appState === "active") {
        refrescarNoLeidos();
        if (!id) id = setInterval(refrescarNoLeidos, INTERVALO_MS);
      } else if (id) {
        clearInterval(id);
        id = null;
      }
    });

    return () => {
      if (id) clearInterval(id);
      sub.remove();
      actualizar({ mensajes: 0, notificaciones: 0 });
    };
  }, []);
}
