package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Enlace de la página de origen a un fichero con el recorrido exacto (GPX, KML...). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnlaceTrackDTO {

    @Size(max = 100, message = "El texto del enlace no puede superar los 100 caracteres")
    private String texto;

    @NotBlank(message = "La URL del enlace es obligatoria")
    @Size(max = 1000, message = "La URL del enlace no puede superar los 1000 caracteres")
    @Pattern(regexp = "https?://.+", message = "La URL del enlace ha de empezar por http:// o https://")
    private String url;
}
