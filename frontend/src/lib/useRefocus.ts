import { useFocusEffect } from "expo-router";
import { useCallback, useEffect, useRef } from "react";

/**
 * Ejecuta `callback` cada vez que la pantalla vuelve a tener el foco (al volver de un
 * detalle o formulario), pero no en el primer montaje, cuando los datos acaban de cargarse.
 */
export function useRefocus(callback: () => void) {
  const primeraVez = useRef(true);
  const callbackRef = useRef(callback);
  useEffect(() => {
    callbackRef.current = callback;
  });

  useFocusEffect(
    useCallback(() => {
      if (primeraVez.current) {
        primeraVez.current = false;
        return;
      }
      callbackRef.current();
    }, [])
  );
}
