package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Representacion ligera de una quedada para listados y feed. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuedadaSummaryDTO {

    private String uuid;

    private String titulo;

    private UsuarioSummaryDTO organizador;

    private String rutaUuid;

    private String rutaNombre;

    private String puntoEncuentro;

    private LocalDateTime fechaHora;

    private Integer maxParticipantes;

    private Long numInscritos;

    private NivelRecomendado nivelRecomendado;

    private EstadoQuedada estado;
}
