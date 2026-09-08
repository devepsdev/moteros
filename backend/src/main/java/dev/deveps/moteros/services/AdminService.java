package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.EstadisticasGlobalesDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.entities.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    EstadisticasGlobalesDTO estadisticasGlobales();

    Page<UsuarioResponseDTO> listarUsuarios(String texto, Pageable pageable);

    UsuarioResponseDTO cambiarRol(String usuarioUuid, RolUsuario rol);
}
