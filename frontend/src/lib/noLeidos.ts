import * as chatApi from "@/api/chat";
import { useEffect, useSyncExternalStore } from "react";
import { AppState } from "react-native";

/**
 * Contador global de mensajes sin leer (insignia de la pestaña Chat). No hay push ni
 * WebSocket: se consulta periódicamente mientras la app está en primer plano y se
 * refresca a mano al abrir o leer una conversación.
 */
let total = 0;
const listeners = new Set<() => void>();

function set(valor: number) {
  if (valor === total) return;
  total = valor;
  listeners.forEach((l) => l());
}

export async function refrescarNoLeidos(): Promise<void> {
  try {
    set(await chatApi.totalNoLeidos());
  } catch {
    // Sin conexión o sesión caducada: se reintenta en el siguiente ciclo.
  }
}

export function useNoLeidos(): number {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => total
  );
}

const INTERVALO_MS = 30_000;

/** Arranca el sondeo del contador. Se monta una sola vez, en el layout de pestañas. */
export function useSondeoNoLeidos() {
  useEffect(() => {
    refrescarNoLeidos();
    let id: ReturnType<typeof setInterval> | null = setInterval(refrescarNoLeidos, INTERVALO_MS);

    const sub = AppState.addEventListener("change", (estado) => {
      if (estado === "active") {
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
      set(0);
    };
  }, []);
}
