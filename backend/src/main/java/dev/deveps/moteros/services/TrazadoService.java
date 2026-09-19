package dev.deveps.moteros.services;

import java.util.List;
import java.util.Optional;

/** Calcula el recorrido por carretera que une los puntos de paso de una ruta. */
public interface TrazadoService {

    /** Con menos puntos no hay tramo que calcular; con mas, se asume un track GPX que ya sigue la carretera. */
    int MIN_PUNTOS = 2;
    int MAX_PUNTOS = 100;

    /**
     * Resultado del calculo.
     *
     * @param polilinea  recorrido codificado (ver {@link dev.deveps.moteros.support.Polilinea});
     *                   vacio si no hay recorrido por carretera entre los puntos
     */
    record Trazado(String polilinea, double distanciaKm, int duracionMin) {
        public boolean encontrado() {
            return !polilinea.isEmpty();
        }
    }

    /** Si hay un servicio de rutas configurado. */
    boolean disponible();

    /**
     * Recorrido por carretera entre los puntos (pares {latitud, longitud}, en orden).
     * Vacio si no se puede calcular ahora (sin servicio, sin cuota o sin conexion): hay que
     * reintentarlo mas tarde. Un {@link Trazado} no encontrado significa que no hay carretera.
     */
    Optional<Trazado> calcular(List<double[]> puntos);
}
