package dev.deveps.moteros.support;

import java.util.List;

/** Medidas sobre la forma de un recorrido (pares {latitud, longitud}). */
public final class Curvas {

    /** Tramos mas cortos no cuentan: el ruido del trazado no son curvas. */
    private static final double TRAMO_MINIMO_KM = 0.03;
    private static final int MAX_MUESTRAS = 600;

    private Curvas() {
    }

    /**
     * Grados que gira la carretera por kilometro: una autovia recta da cerca de 0 y un puerto
     * de montaña varios cientos. Sirve para comparar recorridos entre si, no como medida exacta.
     */
    public static double gradosPorKm(List<double[]> puntos) {
        double km = 0;
        double giro = 0;
        Double rumboPrevio = null;
        double[] ancla = puntos.isEmpty() ? null : puntos.getFirst();
        for (int i = 1; i < puntos.size(); i++) {
            double[] p = puntos.get(i);
            double d = km(ancla, p);
            if (d < TRAMO_MINIMO_KM) {
                continue;
            }
            double rumbo = rumbo(ancla, p);
            if (rumboPrevio != null) {
                double delta = Math.abs(rumbo - rumboPrevio) % 360;
                giro += delta > 180 ? 360 - delta : delta;
            }
            rumboPrevio = rumbo;
            km += d;
            ancla = p;
        }
        return km < 0.5 ? 0 : giro / km;
    }

    /**
     * El punto de {@code recorrido} mas alejado de {@code referencia}: donde mas se separan las
     * dos carreteras. Poner ahi un punto de paso obliga a ir por {@code recorrido}.
     */
    public static double[] puntoMasAlejado(List<double[]> recorrido, List<double[]> referencia) {
        List<double[]> muestras = muestrear(recorrido);
        List<double[]> ref = muestrear(referencia);
        double[] mejor = recorrido.get(recorrido.size() / 2);
        double maxDist = -1;
        for (double[] p : muestras) {
            double minDist = distanciaALinea(p, ref);
            if (minDist > maxDist) {
                maxDist = minDist;
                mejor = p;
            }
        }
        return new double[]{redondear(mejor[0]), redondear(mejor[1])};
    }

    /**
     * Cuanto llega a separarse {@code recorrido} de {@code referencia}: la mayor distancia de un
     * punto del primero al punto mas cercano del segundo (no es simetrica).
     */
    public static double separacionKm(List<double[]> recorrido, List<double[]> referencia) {
        List<double[]> ref = muestrear(referencia);
        double max = 0;
        for (double[] p : muestrear(recorrido)) {
            max = Math.max(max, distanciaALinea(p, ref));
        }
        return max;
    }

    /**
     * Juegos de puntos de paso para obligar al trazado a ir por {@code recorrido} en vez de por
     * {@code referencia}, del mas sencillo al mas insistente: el punto donde mas se separan,
     * puntos antes y despues de ese, y dos puntos a la vez. Todos en el tramo que se aparta.
     */
    public static List<List<double[]>> candidatosDePaso(List<double[]> recorrido, List<double[]> referencia) {
        List<double[]> ref = muestrear(referencia);
        List<double[]> muestras = muestrear(recorrido);
        double[] distancias = new double[muestras.size()];
        double max = 0;
        for (int i = 0; i < muestras.size(); i++) {
            distancias[i] = distanciaALinea(muestras.get(i), ref);
            max = Math.max(max, distancias[i]);
        }
        // El tramo que se aparta: los puntos a mas de la mitad de la separacion maxima.
        java.util.ArrayList<double[]> apartados = new java.util.ArrayList<>();
        int masLejano = 0;
        for (int i = 0; i < muestras.size(); i++) {
            if (distancias[i] >= max / 2) {
                apartados.add(muestras.get(i));
            }
            if (distancias[i] > distancias[masLejano]) {
                masLejano = i;
            }
        }
        if (apartados.isEmpty()) {
            return List.of();
        }
        double[] lejano = r(muestras.get(masLejano));
        double[] primero = r(apartados.get(apartados.size() / 4));
        double[] ultimo = r(apartados.get(apartados.size() * 3 / 4));
        return List.of(List.of(lejano), List.of(primero), List.of(ultimo), List.of(primero, ultimo));
    }

    private static double[] r(double[] p) {
        return new double[]{redondear(p[0]), redondear(p[1])};
    }

    private static List<double[]> muestrear(List<double[]> puntos) {
        if (puntos.size() <= MAX_MUESTRAS) {
            return puntos;
        }
        int paso = (int) Math.ceil(puntos.size() / (double) MAX_MUESTRAS);
        java.util.ArrayList<double[]> muestras = new java.util.ArrayList<>();
        for (int i = 0; i < puntos.size(); i += paso) {
            muestras.add(puntos.get(i));
        }
        return muestras;
    }

    /** Distancia en km de un punto a la linea formada por {@code linea} (a sus tramos, no solo a sus vertices). */
    static double distanciaALinea(double[] p, List<double[]> linea) {
        if (linea.size() == 1) {
            return km(p, linea.getFirst());
        }
        double coseno = Math.cos(Math.toRadians(p[0]));
        double min = Double.MAX_VALUE;
        for (int i = 1; i < linea.size(); i++) {
            double[] a = linea.get(i - 1);
            double[] b = linea.get(i);
            // Coordenadas planas locales (km) con origen en p.
            double ax = Math.toRadians(a[1] - p[1]) * coseno * 6371.0, ay = Math.toRadians(a[0] - p[0]) * 6371.0;
            double bx = Math.toRadians(b[1] - p[1]) * coseno * 6371.0, by = Math.toRadians(b[0] - p[0]) * 6371.0;
            double dx = bx - ax, dy = by - ay;
            double largo2 = dx * dx + dy * dy;
            double t = largo2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / largo2));
            double cx = ax + t * dx, cy = ay + t * dy;
            min = Math.min(min, Math.sqrt(cx * cx + cy * cy));
        }
        return min;
    }

    /** Distancia aproximada en km (proyeccion equirectangular: de sobra para distancias cortas). */
    static double km(double[] a, double[] b) {
        double x = Math.toRadians(b[1] - a[1]) * Math.cos(Math.toRadians((a[0] + b[0]) / 2));
        double y = Math.toRadians(b[0] - a[0]);
        return Math.sqrt(x * x + y * y) * 6371.0;
    }

    private static double rumbo(double[] a, double[] b) {
        double x = Math.toRadians(b[1] - a[1]) * Math.cos(Math.toRadians((a[0] + b[0]) / 2));
        double y = Math.toRadians(b[0] - a[0]);
        return Math.toDegrees(Math.atan2(x, y));
    }

    /** El backend guarda las coordenadas con 7 decimales como mucho. */
    private static double redondear(double grados) {
        return Math.round(grados * 1e6) / 1e6;
    }
}
