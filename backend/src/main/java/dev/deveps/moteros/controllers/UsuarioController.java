package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.UsuarioRequestDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.dto.UsuarioSearchDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UsuarioSummaryDTO>>> buscar(
            @Valid UsuarioSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "nombreUsuario");
        Page<UsuarioSummaryDTO> pagina = usuarioService.buscar(searchDTO.getSearchText(), pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(pagina), "Usuarios obtenidos correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> miPerfil() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                usuarioService.obtenerPerfilActual(), "Perfil obtenido correctamente"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> actualizarPerfil(
            @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                usuarioService.actualizarPerfil(dto), "Perfil actualizado correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                usuarioService.obtenerPorUuid(uuid), "Usuario obtenido correctamente"));
    }
}
