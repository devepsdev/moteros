package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.TipoMoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Moto con todos sus datos y el propietario anidado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotoResponseDTO {

    private String uuid;

    private UsuarioSummaryDTO propietario;

    private String marca;

    private String modelo;

    private Short anio;

    private Integer cilindradaCc;

    private TipoMoto tipo;

    private String fotoUrl;
}
