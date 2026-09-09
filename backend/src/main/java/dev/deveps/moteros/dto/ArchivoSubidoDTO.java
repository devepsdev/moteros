package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de subir un archivo. La {@code url} es la que se guarda luego en el recurso (foto de perfil, moto, publicacion...). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoSubidoDTO {

    private String url;

    private String nombreArchivo;

    private String tipoContenido;

    private long tamano;
}
