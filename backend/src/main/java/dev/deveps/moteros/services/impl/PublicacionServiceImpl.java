package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.ComentarioRequestDTO;
import dev.deveps.moteros.dto.ComentarioResponseDTO;
import dev.deveps.moteros.dto.LikePublicacionResponseDTO;
import dev.deveps.moteros.dto.PublicacionRequestDTO;
import dev.deveps.moteros.dto.PublicacionResponseDTO;
import dev.deveps.moteros.entities.Comentario;
import dev.deveps.moteros.entities.LikePublicacion;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ComentarioRepository;
import dev.deveps.moteros.repositories.LikePublicacionRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.ValoracionRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import dev.deveps.moteros.services.PublicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicacionServiceImpl implements PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ComentarioRepository comentarioRepository;
    private final LikePublicacionRepository likeRepository;
    private final RutaRepository rutaRepository;
    private final ValoracionRutaRepository valoracionRutaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final NotificacionService notificacionService;
    private final EntityDtoMapper mapper;

    // ===================== CONSULTAS =====================

    @Override
    @Transactional(readOnly = true)
    public Page<PublicacionResponseDTO> feed(Pageable pageable) {
        Integer usuarioId = usuarioAutenticado.obtenerIdUsuarioActual();
        return publicacionRepository.feed(usuarioId, pageable).map(p -> toResponse(p, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicacionResponseDTO> buscar(String texto, Pageable pageable) {
        return publicacionRepository.buscarPorTexto(texto, pageable).map(p -> toResponse(p, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicacionResponseDTO> listarPorUsuario(String usuarioUuid, Pageable pageable) {
        return publicacionRepository
                .findByUsuarioUuidOrderByFechaPublicacionDesc(usuarioUuid, pageable)
                .map(p -> toResponse(p, true));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicacionResponseDTO obtenerPorUuid(String uuid) {
        return toResponse(buscar(uuid), true);
    }

    // ===================== ALTA / EDICION =====================

    @Override
    public PublicacionResponseDTO crear(PublicacionRequestDTO dto) {
        Usuario autor = usuarioAutenticado.obtenerUsuarioActual();
        Publicacion publicacion = Publicacion.builder()
                .usuario(autor)
                .ruta(resolverRuta(dto.getRutaUuid()))
                .contenido(dto.getContenido())
                .imagenUrl(dto.getImagenUrl())
                .build();
        return toResponse(publicacionRepository.save(publicacion), true);
    }

    @Override
    public PublicacionResponseDTO actualizar(String uuid, PublicacionRequestDTO dto) {
        Publicacion publicacion = buscar(uuid);
        exigirAutor(publicacion);
        publicacion.setContenido(dto.getContenido());
        publicacion.setImagenUrl(dto.getImagenUrl());
        publicacion.setRuta(resolverRuta(dto.getRutaUuid()));
        return toResponse(publicacionRepository.save(publicacion), true);
    }

    @Override
    public void eliminar(String uuid) {
        Publicacion publicacion = buscar(uuid);
        exigirAutor(publicacion);
        publicacionRepository.delete(publicacion);
    }

    // ===================== COMENTARIOS =====================

    @Override
    @Transactional(readOnly = true)
    public Page<ComentarioResponseDTO> listarComentarios(String publicacionUuid, Pageable pageable) {
        buscar(publicacionUuid);
        return comentarioRepository
                .findByPublicacionUuidOrderByFechaAsc(publicacionUuid, pageable)
                .map(mapper::comentarioResponse);
    }

    @Override
    public ComentarioResponseDTO comentar(String publicacionUuid, ComentarioRequestDTO dto) {
        Publicacion publicacion = buscar(publicacionUuid);
        Usuario autor = usuarioAutenticado.obtenerUsuarioActual();
        Comentario comentario = Comentario.builder()
                .publicacion(publicacion)
                .usuario(autor)
                .contenido(dto.getContenido())
                .build();
        Comentario guardado = comentarioRepository.save(comentario);

        notificacionService.notificar(publicacion.getUsuario(), TipoNotificacion.comentario,
                publicacion.getId(), autor,
                autor.getNombreCompleto() + " ha comentado tu publicacion.");

        return mapper.comentarioResponse(guardado);
    }

    @Override
    public void eliminarComentario(String comentarioUuid) {
        Comentario comentario = comentarioRepository.findByUuid(comentarioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado: " + comentarioUuid));
        Integer actual = usuarioAutenticado.obtenerIdUsuarioActual();
        boolean autorComentario = comentario.getUsuario().getId().equals(actual);
        boolean autorPublicacion = comentario.getPublicacion().getUsuario().getId().equals(actual);
        if (!autorComentario && !autorPublicacion) {
            throw new BadRequestException("No puedes eliminar este comentario");
        }
        comentarioRepository.delete(comentario);
    }

    // ===================== LIKES =====================

    @Override
    public boolean alternarLike(String publicacionUuid) {
        Publicacion publicacion = buscar(publicacionUuid);
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();

        return likeRepository.findByPublicacionIdAndUsuarioId(publicacion.getId(), usuario.getId())
                .map(existente -> {
                    likeRepository.delete(existente);
                    return false;
                })
                .orElseGet(() -> {
                    likeRepository.save(LikePublicacion.builder()
                            .publicacion(publicacion)
                            .usuario(usuario)
                            .build());
                    notificacionService.notificar(publicacion.getUsuario(), TipoNotificacion.like,
                            publicacion.getId(), usuario,
                            "A " + usuario.getNombreCompleto() + " le ha gustado tu publicacion.");
                    return true;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikePublicacionResponseDTO> listarLikes(String publicacionUuid, Pageable pageable) {
        buscar(publicacionUuid);
        return likeRepository
                .findByPublicacionUuidOrderByFechaDesc(publicacionUuid, pageable)
                .map(mapper::likeResponse);
    }

    // ===================== PRIVADOS =====================

    private Publicacion buscar(String uuid) {
        return publicacionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Publicacion no encontrada: " + uuid));
    }

    private void exigirAutor(Publicacion publicacion) {
        if (!publicacion.getUsuario().getId().equals(usuarioAutenticado.obtenerIdUsuarioActual())) {
            throw new BadRequestException("No puedes modificar una publicacion que no es tuya");
        }
    }

    private Ruta resolverRuta(String rutaUuid) {
        if (rutaUuid == null || rutaUuid.isBlank()) {
            return null;
        }
        return rutaRepository.findByUuid(rutaUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + rutaUuid));
    }

    private PublicacionResponseDTO toResponse(Publicacion p, boolean incluirComentarios) {
        Integer id = p.getId();
        long numLikes = likeRepository.countByPublicacionId(id);
        long numComentarios = comentarioRepository.countByPublicacionId(id);
        boolean likeActual = likeRepository.existsByPublicacionIdAndUsuarioId(
                id, usuarioAutenticado.obtenerIdUsuarioActual());

        List<ComentarioResponseDTO> comentarios = null;
        if (incluirComentarios) {
            comentarios = comentarioRepository.findTop3ByPublicacionIdOrderByFechaDesc(id).stream()
                    .map(mapper::comentarioResponse)
                    .toList();
        }

        Double rutaMedia = null;
        Long rutaNumVal = null;
        if (p.getRuta() != null) {
            rutaMedia = valoracionRutaRepository.mediaPuntuacion(p.getRuta().getId());
            if (rutaMedia != null) {
                rutaMedia = Math.round(rutaMedia * 100.0) / 100.0;
            }
            rutaNumVal = valoracionRutaRepository.countByRutaId(p.getRuta().getId());
        }

        return mapper.publicacionResponse(p, numLikes, numComentarios, likeActual, comentarios, rutaMedia, rutaNumVal);
    }
}
