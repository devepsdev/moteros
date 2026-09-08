package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.ComentarioRequestDTO;
import dev.deveps.moteros.dto.ComentarioResponseDTO;
import dev.deveps.moteros.dto.LikePublicacionResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.PublicacionRequestDTO;
import dev.deveps.moteros.dto.PublicacionResponseDTO;
import dev.deveps.moteros.dto.PublicacionSearchDTO;
import dev.deveps.moteros.services.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;

    @GetMapping("/feed")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<PublicacionResponseDTO>>> feed(
            @Valid PublicacionSearchDTO searchDTO) {
        Pageable pageable = pageable(searchDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(publicacionService.feed(pageable)), "Feed obtenido correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<PublicacionResponseDTO>>> buscar(
            @Valid PublicacionSearchDTO searchDTO) {
        Pageable pageable = pageable(searchDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(publicacionService.buscar(searchDTO.getSearchText(), pageable)),
                "Publicaciones obtenidas correctamente"));
    }

    @GetMapping("/usuario/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<PublicacionResponseDTO>>> porUsuario(
            @PathVariable String usuarioUuid, @Valid PublicacionSearchDTO searchDTO) {
        Pageable pageable = pageable(searchDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(publicacionService.listarPorUsuario(usuarioUuid, pageable)),
                "Publicaciones del usuario obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<PublicacionResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                publicacionService.obtenerPorUuid(uuid), "Publicacion obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PublicacionResponseDTO>> crear(
            @Valid @RequestBody PublicacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(publicacionService.crear(dto), "Publicacion creada correctamente"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<PublicacionResponseDTO>> actualizar(
            @PathVariable String uuid, @Valid @RequestBody PublicacionRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                publicacionService.actualizar(uuid, dto), "Publicacion actualizada correctamente"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable String uuid) {
        publicacionService.eliminar(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Publicacion eliminada correctamente"));
    }

    // ===================== COMENTARIOS =====================

    @GetMapping("/{uuid}/comentarios")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ComentarioResponseDTO>>> listarComentarios(
            @PathVariable String uuid, @Valid PublicacionSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fecha");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(publicacionService.listarComentarios(uuid, pageable)),
                "Comentarios obtenidos correctamente"));
    }

    @PostMapping("/{uuid}/comentarios")
    public ResponseEntity<ApiResponseDTO<ComentarioResponseDTO>> comentar(
            @PathVariable String uuid, @Valid @RequestBody ComentarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(publicacionService.comentar(uuid, dto), "Comentario publicado"));
    }

    @DeleteMapping("/comentarios/{comentarioUuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminarComentario(@PathVariable String comentarioUuid) {
        publicacionService.eliminarComentario(comentarioUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Comentario eliminado correctamente"));
    }

    // ===================== LIKES =====================

    @PutMapping("/{uuid}/like")
    public ResponseEntity<ApiResponseDTO<Map<String, Boolean>>> alternarLike(@PathVariable String uuid) {
        boolean conLike = publicacionService.alternarLike(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("like", conLike), conLike ? "Te gusta esta publicacion" : "Ya no te gusta esta publicacion"));
    }

    @GetMapping("/{uuid}/likes")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<LikePublicacionResponseDTO>>> listarLikes(
            @PathVariable String uuid, @Valid PublicacionSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fecha");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(publicacionService.listarLikes(uuid, pageable)),
                "Likes obtenidos correctamente"));
    }

    private static Pageable pageable(PublicacionSearchDTO s) {
        return PageableFactory.of(s.getPage(), s.getSize(), s.getSortBy(), s.getSortDir(), "fechaPublicacion");
    }
}
