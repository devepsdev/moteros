package dev.deveps.moteros.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Polilinea codificada (algoritmo de Google, precision 5): una lista de coordenadas en un
 * texto compacto. La app y el panel la decodifican para dibujar el trazado.
 */
public final class Polilinea {

    private Polilinea() {
    }

    /** Coordenadas como pares {latitud, longitud}. */
    public static String codificar(List<double[]> puntos) {
        StringBuilder sb = new StringBuilder();
        long latPrevia = 0;
        long lonPrevia = 0;
        for (double[] p : puntos) {
            long lat = Math.round(p[0] * 1e5);
            long lon = Math.round(p[1] * 1e5);
            escribir(lat - latPrevia, sb);
            escribir(lon - lonPrevia, sb);
            latPrevia = lat;
            lonPrevia = lon;
        }
        return sb.toString();
    }

    public static List<double[]> decodificar(String texto) {
        List<double[]> puntos = new ArrayList<>();
        int[] indice = {0};
        long lat = 0;
        long lon = 0;
        while (indice[0] < texto.length()) {
            lat += leer(texto, indice);
            lon += leer(texto, indice);
            puntos.add(new double[]{lat / 1e5, lon / 1e5});
        }
        return puntos;
    }

    private static void escribir(long valor, StringBuilder sb) {
        long v = valor < 0 ? ~(valor << 1) : valor << 1;
        while (v >= 0x20) {
            sb.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        sb.append((char) (v + 63));
    }

    private static long leer(String texto, int[] indice) {
        long resultado = 0;
        int desplazamiento = 0;
        int b;
        do {
            b = texto.charAt(indice[0]++) - 63;
            resultado |= (long) (b & 0x1f) << desplazamiento;
            desplazamiento += 5;
        } while (b >= 0x20);
        return (resultado & 1) != 0 ? ~(resultado >> 1) : resultado >> 1;
    }
}
