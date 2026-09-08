package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.EstadisticasGlobalesDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de administracion. Toda la ruta {@code /api/admin/**} requiere rol ADMIN
 * (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponseDTO<EstadisticasGlobalesDTO>> estadisticas() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                adminService.estadisticasGlobales(), "Estadisticas obtenidas correctamente"));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UsuarioResponseDTO>>> listarUsuarios(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fechaRegistro", "desc", "fechaRegistro");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(adminService.listarUsuarios(texto, pageable)),
                "Usuarios obtenidos correctamente"));
    }

    @PatchMapping("/usuarios/{uuid}/rol")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> cambiarRol(
            @PathVariable String uuid, @RequestParam RolUsuario rol) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                adminService.cambiarRol(uuid, rol), "Rol actualizado correctamente"));
    }
}
