package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Filtro avanzado de quedadas: por nivel, estado, ruta, organizador, rango de fechas
 * y cercania geografica al punto de encuentro (lat/lng + radio en km).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuedadaFilterDTO {

    private String titulo;

    private NivelRecomendado nivelRecomendado;

    private EstadoQuedada estado;

    private String rutaUuid;

    private String organizadorUuid;

    private LocalDateTime fechaDesde;

    private LocalDateTime fechaHasta;

    /** Si es true, solo quedadas con {@code fechaHora} posterior a ahora. */
    private Boolean soloProximas;

    // ===== CERCANIA GEOGRAFICA (los tres juntos o ninguno) =====
    @DecimalMin(value = "-90.0", message = "La latitud ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud ha de estar entre -90 y 90")
    private BigDecimal latitud;

    @DecimalMin(value = "-180.0", message = "La longitud ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud ha de estar entre -180 y 180")
    private BigDecimal longitud;

    private Double radioKm;

    // ===== PAGINACION =====
    @Min(value = 0, message = "El numero de pagina ha de ser 0 o superior")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "El tamano de pagina ha de ser 1 o superior")
    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "fechaHora";

    @Pattern(regexp = "asc|desc", message = "La direccion de ordenacion ha de ser 'asc' o 'desc'")
    @Builder.Default
    private String sortDir = "asc";

    public boolean hasGeoFilter() {
        return latitud != null && longitud != null && radioKm != null;
    }

    public boolean hasTextFilters() {
        return titulo != null && !titulo.trim().isEmpty();
    }
}
