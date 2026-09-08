package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.TipoMoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Representacion ligera de una moto para anidar en el perfil de un usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotoSummaryDTO {

    private String uuid;

    private String marca;

    private String modelo;

    private Short anio;

    private TipoMoto tipo;

    private String fotoUrl;
}
