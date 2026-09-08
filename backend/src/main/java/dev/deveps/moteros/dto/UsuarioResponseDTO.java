package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Perfil publico de un usuario. Nunca expone {@code id} ni {@code passwordHash}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private String uuid;

    private String nombreUsuario;

    private String nombreCompleto;

    private String email;

    private String ciudad;

    private String biografia;

    private String fotoPerfilUrl;

    private LocalDateTime fechaRegistro;

    private Boolean activo;

    private RolUsuario rol;

    // ===== CONTADORES (opcionales, los rellena el servicio si procede) =====
    private Long numMotos;

    private Long numRutas;

    private Long numAmigos;
}
