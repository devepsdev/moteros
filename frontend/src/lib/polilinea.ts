/**
 * Decodifica una polilínea (algoritmo de Google, precisión 5), el formato en que la API
 * devuelve el recorrido por carretera de una ruta.
 */
export function decodificarPolilinea(texto: string): { latitude: number; longitude: number }[] {
  const puntos: { latitude: number; longitude: number }[] = [];
  let indice = 0;
  let lat = 0;
  let lon = 0;

  const leer = () => {
    let resultado = 0;
    let desplazamiento = 0;
    let b: number;
    do {
      b = texto.charCodeAt(indice++) - 63;
      resultado |= (b & 0x1f) << desplazamiento;
      desplazamiento += 5;
    } while (b >= 0x20);
    return resultado & 1 ? ~(resultado >> 1) : resultado >> 1;
  };

  while (indice < texto.length) {
    lat += leer();
    lon += leer();
    puntos.push({ latitude: lat / 1e5, longitude: lon / 1e5 });
  }
  return puntos;
}
