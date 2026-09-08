package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Busqueda paginada de usuarios por texto (nombre de usuario, nombre completo o ciudad). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSearchDTO {

    private String searchText;

    @Min(value = 0, message = "El numero de pagina ha de ser 0 o superior")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "El tamano de pagina ha de ser 1 o superior")
    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "nombreUsuario";

    @Pattern(regexp = "asc|desc", message = "La direccion de ordenacion ha de ser 'asc' o 'desc'")
    @Builder.Default
    private String sortDir = "asc";
}
