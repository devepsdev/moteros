package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.NotificacionResponseDTO;
import dev.deveps.moteros.entities.Notificacion;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.NotificacionRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacionResponseDTO> listar(boolean soloNoLeidas, Pageable pageable) {
        String uuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        var pagina = soloNoLeidas
                ? notificacionRepository.findByUsuarioUuidAndLeidoFalseOrderByFechaCreacionDesc(uuid, pageable)
                : notificacionRepository.findByUsuarioUuidOrderByFechaCreacionDesc(uuid, pageable);
        return pagina.map(mapper::notificacionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        return notificacionRepository.countByUsuarioIdAndLeidoFalse(usuarioAutenticado.obtenerIdUsuarioActual());
    }

    @Override
    public void marcarLeida(String uuid) {
        Notificacion n = notificacionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada: " + uuid));
        if (!n.getUsuario().getId().equals(usuarioAutenticado.obtenerIdUsuarioActual())) {
            throw new BadRequestException("Esta notificacion no es tuya");
        }
        n.setLeido(true);
        notificacionRepository.save(n);
    }

    @Override
    public void marcarTodasLeidas() {
        notificacionRepository.marcarTodasLeidas(usuarioAutenticado.obtenerIdUsuarioActual());
    }

    @Override
    public void notificar(Usuario destino, TipoNotificacion tipo, Integer referenciaId,
                          Usuario origen, String mensaje) {
        if (destino == null) {
            return;
        }
        if (origen != null && destino.getId().equals(origen.getId())) {
            return;
        }
        Notificacion n = Notificacion.builder()
                .usuario(destino)
                .tipo(tipo)
                .referenciaId(referenciaId)
                .usuarioOrigen(origen)
                .mensaje(mensaje)
                .leido(false)
                .build();
        notificacionRepository.save(n);
    }
}
