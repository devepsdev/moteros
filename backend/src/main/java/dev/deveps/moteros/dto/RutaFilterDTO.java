package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Filtro avanzado de rutas: por dificultad, terreno, rango de distancia/duracion,
 * creador y cercania geografica a un punto (lat/lng + radio en km).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaFilterDTO {

    private String nombre;

    private Dificultad dificultad;

    private TipoTerreno tipoTerreno;

    private BigDecimal distanciaMinKm;

    private BigDecimal distanciaMaxKm;

    private Integer duracionMaxMin;

    private String creadorUuid;

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
    private String sortBy = "fechaCreacion";

    @Pattern(regexp = "asc|desc", message = "La direccion de ordenacion ha de ser 'asc' o 'desc'")
    @Builder.Default
    private String sortDir = "desc";

    /** true si se han informado latitud, longitud y radio para filtrar por cercania. */
    public boolean hasGeoFilter() {
        return latitud != null && longitud != null && radioKm != null;
    }

    public boolean hasTextFilters() {
        return nombre != null && !nombre.trim().isEmpty();
    }
}
