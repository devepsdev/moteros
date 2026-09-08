package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.AmistadResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AmistadService;
import dev.deveps.moteros.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AmistadServiceImpl implements AmistadService {

    private final AmistadRepository amistadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final NotificacionService notificacionService;
    private final EntityDtoMapper mapper;

    @Override
    public AmistadResponseDTO enviarSolicitud(String usuarioUuid) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        Usuario otro = usuarioPorUuid(usuarioUuid);

        if (yo.getId().equals(otro.getId())) {
            throw new BadRequestException("No puedes enviarte una solicitud a ti mismo");
        }

        Amistad amistad = amistadRepository.findRelacion(yo.getId(), otro.getId()).orElse(null);
        if (amistad != null) {
            if (amistad.getEstado() == EstadoAmistad.aceptada) {
                throw new BadRequestException("Ya sois amigos");
            }
            if (amistad.getEstado() == EstadoAmistad.pendiente) {
                throw new BadRequestException("Ya hay una solicitud pendiente con este usuario");
            }
            // estaba rechazada: se reutiliza la fila reabriendo la solicitud
            amistad.setUsuario(yo);
            amistad.setAmigo(otro);
            amistad.setEstado(EstadoAmistad.pendiente);
        } else {
            amistad = Amistad.builder()
                    .usuario(yo)
                    .amigo(otro)
                    .estado(EstadoAmistad.pendiente)
                    .build();
        }

        Amistad guardada = amistadRepository.save(amistad);
        notificacionService.notificar(otro, TipoNotificacion.solicitud_amistad, null, yo,
                yo.getNombreCompleto() + " te ha enviado una solicitud de amistad.");
        return mapper.amistadResponse(guardada, yo.getId());
    }

    @Override
    public AmistadResponseDTO responder(String amistadUuid, boolean aceptar) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        Amistad amistad = amistadRepository.findByUuid(amistadUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + amistadUuid));

        if (!amistad.getAmigo().getId().equals(yo.getId())) {
            throw new BadRequestException("Solo el destinatario puede responder a esta solicitud");
        }
        if (amistad.getEstado() != EstadoAmistad.pendiente) {
            throw new BadRequestException("Esta solicitud ya ha sido respondida");
        }

        amistad.setEstado(aceptar ? EstadoAmistad.aceptada : EstadoAmistad.rechazada);
        Amistad guardada = amistadRepository.save(amistad);

        if (aceptar) {
            notificacionService.notificar(amistad.getUsuario(), TipoNotificacion.amistad_aceptada, null, yo,
                    yo.getNombreCompleto() + " ha aceptado tu solicitud de amistad.");
        }
        return mapper.amistadResponse(guardada, yo.getId());
    }

    @Override
    public void eliminar(String usuarioUuid) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Usuario otro = usuarioPorUuid(usuarioUuid);
        Amistad amistad = amistadRepository.findRelacion(yoId, otro.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe relacion con este usuario"));
        amistadRepository.delete(amistad);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioSummaryDTO> listarAmigos(String usuarioUuid, Pageable pageable) {
        Usuario usuario = usuarioPorUuid(usuarioUuid);
        return amistadRepository.findAmigosAceptados(usuario.getId(), pageable)
                .map(mapper::usuarioSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AmistadResponseDTO> solicitudesRecibidas(Pageable pageable) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        String yoUuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        return amistadRepository.findByAmigoUuidAndEstado(yoUuid, EstadoAmistad.pendiente, pageable)
                .map(a -> mapper.amistadResponse(a, yoId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AmistadResponseDTO> solicitudesEnviadas(Pageable pageable) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        String yoUuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        return amistadRepository.findByUsuarioUuidAndEstado(yoUuid, EstadoAmistad.pendiente, pageable)
                .map(a -> mapper.amistadResponse(a, yoId));
    }

    @Override
    @Transactional(readOnly = true)
    public AmistadResponseDTO relacionCon(String usuarioUuid) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Usuario otro = usuarioPorUuid(usuarioUuid);
        return amistadRepository.findRelacion(yoId, otro.getId())
                .map(a -> mapper.amistadResponse(a, yoId))
                .orElse(null);
    }

    private Usuario usuarioPorUuid(String uuid) {
        return usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + uuid));
    }
}
