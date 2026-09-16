package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Medidas que toma el administrador. Sin ninguna, la denuncia queda descartada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolverDenunciaDTO {

    /** Borra el contenido; en un perfil, quita la foto y la biografia. */
    private boolean eliminarContenido;

    private boolean darDeBaja;

    @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
    private String nota;
}
