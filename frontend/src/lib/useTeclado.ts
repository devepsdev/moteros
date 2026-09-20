import { useEffect, useState } from "react";
import { Dimensions, Keyboard, Platform, useWindowDimensions } from "react-native";

/**
 * Cuánto hay que subir el contenido para que el teclado no lo tape.
 *
 * En Android, según la versión y el modo a pantalla completa, el sistema encoge la ventana al
 * abrir el teclado o la deja igual. Aquí se mide: si la ventana ha encogido, el sistema ya ha
 * hecho parte del trabajo y solo se devuelve lo que falta; si no, la altura entera del teclado.
 */
export function useDesplazamientoTeclado(): number {
  const { height } = useWindowDimensions();
  const [{ teclado, alturaLibre }, setEstado] = useState({
    teclado: 0,
    alturaLibre: Dimensions.get("window").height,
  });

  useEffect(() => {
    const abrir = Keyboard.addListener(Platform.OS === "ios" ? "keyboardWillShow" : "keyboardDidShow", (e) =>
      setEstado((previo) => ({ ...previo, teclado: e.endCoordinates.height }))
    );
    const cerrar = Keyboard.addListener(Platform.OS === "ios" ? "keyboardWillHide" : "keyboardDidHide", () =>
      // Con el teclado cerrado, la ventana está entera: sirve de referencia para la próxima vez.
      setEstado({ teclado: 0, alturaLibre: Dimensions.get("window").height })
    );
    return () => {
      abrir.remove();
      cerrar.remove();
    };
  }, []);

  const encogida = Math.max(0, alturaLibre - height);
  return Math.max(0, teclado - encogida);
}
