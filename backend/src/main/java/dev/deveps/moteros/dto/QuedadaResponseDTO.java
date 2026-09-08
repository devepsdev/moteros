package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Quedada completa: datos, geolocalizacion del punto de encuentro, ruta asociada e inscritos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuedadaResponseDTO {

    private String uuid;

    private UsuarioSummaryDTO organizador;

    /** Ruta asociada. Puede ser null (quedada sin ruta). */
    private RutaSummaryDTO ruta;

    private String titulo;

    private String descripcion;

    private String puntoEncuentro;

    private BigDecimal latitudEncuentro;

    private BigDecimal longitudEncuentro;

    private LocalDateTime fechaHora;

    private Integer maxParticipantes;

    private NivelRecomendado nivelRecomendado;

    private EstadoQuedada estado;

    private LocalDateTime fechaCreacion;

    // ===== CALCULADOS =====
    private Long numInscritos;

    private Integer plazasLibres;

    /** Estado de inscripcion del usuario que consulta (null si no esta inscrito). */
    private EstadoInscripcion inscripcionUsuarioActual;

    /** Lista de inscritos. Puede venir vacia en listados. */
    private List<InscripcionQuedadaResponseDTO> inscritos;
}
