package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Inscripcion de un usuario a una quedada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionQuedadaResponseDTO {

    private String uuid;

    private String quedadaUuid;

    private UsuarioSummaryDTO usuario;

    private EstadoInscripcion estado;

    private LocalDateTime fechaInscripcion;
}
