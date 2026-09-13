import * as publicacionesApi from "@/api/publicaciones";
import type { Publicacion } from "@/types/dto";
import { useCallback } from "react";
import { Alert } from "react-native";
import { describeError } from "./errors";

/** Alterna el like en local al instante y lo confirma con el servidor (revierte si falla). */
export function useAlternarLike(updateItems: (updater: (items: Publicacion[]) => Publicacion[]) => void) {
  return useCallback(
    async (p: Publicacion) => {
      const aplicar = (like: boolean) =>
        updateItems((items) =>
          items.map((x) =>
            x.uuid === p.uuid
              ? { ...x, likeUsuarioActual: like, numLikes: Math.max(0, (x.numLikes ?? 0) + (like ? 1 : -1)) }
              : x
          )
        );
      const nuevo = !p.likeUsuarioActual;
      aplicar(nuevo);
      try {
        await publicacionesApi.alternarLike(p.uuid);
      } catch (cause) {
        aplicar(!nuevo);
        Alert.alert("No se ha podido guardar", describeError(cause).message);
      }
    },
    [updateItems]
  );
}
