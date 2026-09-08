package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Panel global de estadisticas de la plataforma. Solo accesible por administradores. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasGlobalesDTO {

    // ===== TOTALES =====
    private long usuariosTotales;

    private long usuariosActivos;

    private long administradores;

    private long motos;

    private long rutas;

    private long quedadas;

    private long quedadasProximas;

    private long publicaciones;

    private long comentarios;

    private long valoraciones;

    private long amistadesAceptadas;

    // ===== DESGLOSES (clave = valor del enum) =====
    private Map<String, Long> rutasPorDificultad;

    private Map<String, Long> rutasPorTerreno;

    private Map<String, Long> motosPorTipo;

    private Map<String, Long> quedadasPorEstado;

    // ===== RANKINGS =====
    private List<RutaSummaryDTO> topRutasPorValoracion;

    private List<AltasMesDTO> altasUsuariosPorMes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AltasMesDTO {

        /** Mes en formato 'YYYY-MM'. */
        private String mes;

        private long total;
    }
}
