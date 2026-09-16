package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.MotivoDenuncia;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Denuncia de un contenido desde la app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenunciaRequestDTO {

    @NotNull(message = "Indica que quieres denunciar")
    private TipoDenuncia tipo;

    @NotBlank(message = "Falta el contenido denunciado")
    @Size(max = 36, message = "Identificador no valido")
    private String referenciaUuid;

    @NotNull(message = "Indica el motivo de la denuncia")
    private MotivoDenuncia motivo;

    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    private String descripcion;
}
