package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una de las carreteras posibles entre dos puntos seguidos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativaDTO {

    /** Polilinea codificada (precision 5). */
    private String trazado;
    private double distanciaKm;
    private int duracionMin;
    /** Grados de giro por kilometro: sirve para comparar cual tiene mas curvas. */
    private double curvasPorKm;
    private boolean masCurvas;
    private boolean masRapida;
    /**
     * Puntos sobre esta carretera que hay que añadir a la ruta entre los dos del tramo (marcados
     * como {@code via}) para que el trazado la siga. Vacio en la primera: sale sin añadir nada.
     */
    private java.util.List<CoordenadaDTO> puntosDePaso;
}
