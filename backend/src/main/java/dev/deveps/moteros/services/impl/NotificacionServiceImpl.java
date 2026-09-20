package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.NotificacionResponseDTO;
import dev.deveps.moteros.entities.Notificacion;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ConversacionRepository;
import dev.deveps.moteros.repositories.NotificacionRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import dev.deveps.moteros.services.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@lombok.extern.slf4j.Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final QuedadaRepository quedadaRepository;
    private final ConversacionRepository conversacionRepository;
    private final RutaRepository rutaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final PushService pushService;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacionResponseDTO> listar(boolean soloNoLeidas, Pageable pageable) {
        String uuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        var pagina = soloNoLeidas
                ? notificacionRepository.findByUsuarioUuidAndLeidoFalseOrderByFechaCreacionDesc(uuid, pageable)
                : notificacionRepository.findByUsuarioUuidOrderByFechaCreacionDesc(uuid, pageable);
        var dtos = pagina.map(mapper::notificacionResponse);
        resolverReferencias(dtos.getContent());
        return dtos;
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
        if (tipo == TipoNotificacion.mensaje && referenciaId != null) {
            // Una sola notificacion por conversacion sin leer: la anterior se sustituye por la
            // nueva para que suba arriba con la fecha del ultimo mensaje.
            notificacionRepository
                    .findFirstByUsuarioIdAndTipoAndReferenciaIdAndLeidoFalse(destino.getId(), tipo, referenciaId)
                    .ifPresent(notificacionRepository::delete);
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
        avisarAlMovil(destino, tipo, referenciaId, origen, mensaje);
    }

    /**
     * El mismo aviso, al movil del destinatario. Se manda al confirmar la transaccion: si la
     * operacion se deshace, nadie recibe un aviso de algo que no ha pasado.
     */
    private void avisarAlMovil(Usuario destino, TipoNotificacion tipo, Integer referenciaId,
                               Usuario origen, String mensaje) {
        String quien = origen == null ? null : origen.getNombreCompleto();
        String titulo = tituloPush(tipo, quien);
        String cuerpo = mensaje != null && !mensaje.isBlank() ? mensaje : titulo;
        Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("tipo", tipo.name());
        if (referenciaId != null) {
            datos.put("referenciaId", referenciaId);
        }
        Runnable enviar = () -> {
            try {
                pushService.enviar(destino, titulo, cuerpo, datos);
            } catch (RuntimeException e) {
                // Un fallo avisando al movil no puede afectar a lo que ya esta guardado.
                log.warn("No se ha podido avisar al movil: {}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar.run();
                }
            });
        } else {
            enviar.run();
        }
    }

    /** Titulo corto para la barra de notificaciones del movil. */
    private static String tituloPush(TipoNotificacion tipo, String quien) {
        String alguien = quien == null || quien.isBlank() ? "Alguien" : quien;
        return switch (tipo) {
            case like -> alguien + " le ha dado a me gusta";
            case comentario -> alguien + " ha comentado tu publicacion";
            case solicitud_amistad -> alguien + " quiere seguirte";
            case amistad_aceptada -> alguien + " ha aceptado tu solicitud";
            case inscripcion_quedada -> alguien + " se ha apuntado a tu quedada";
            case nueva_quedada -> "Nueva quedada de " + alguien;
            case quedada_cancelada -> "Quedada cancelada";
            case mensaje -> "Mensaje de " + alguien;
            case valoracion_ruta -> alguien + " ha valorado tu ruta";
        };
    }

    @Override
    public void marcarLeidasDeConversacion(Integer usuarioId, Integer conversacionId) {
        notificacionRepository.marcarLeidasPorReferencia(usuarioId, TipoNotificacion.mensaje, conversacionId);
    }

    /** Rellena referenciaUuid con una consulta por tipo de entidad para toda la pagina. */
    private void resolverReferencias(List<NotificacionResponseDTO> notificaciones) {
        Map<Integer, String> publicaciones = uuids(ids(notificaciones, TipoNotificacion.like, TipoNotificacion.comentario),
                ids -> publicacionRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(x -> x.getId(), x -> x.getUuid())));
        Map<Integer, String> quedadas = uuids(ids(notificaciones, TipoNotificacion.nueva_quedada,
                        TipoNotificacion.inscripcion_quedada, TipoNotificacion.quedada_cancelada),
                ids -> quedadaRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(x -> x.getId(), x -> x.getUuid())));
        Map<Integer, String> conversaciones = uuids(ids(notificaciones, TipoNotificacion.mensaje),
                ids -> conversacionRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(x -> x.getId(), x -> x.getUuid())));
        Map<Integer, String> rutas = uuids(ids(notificaciones, TipoNotificacion.valoracion_ruta),
                ids -> rutaRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(x -> x.getId(), x -> x.getUuid())));

        for (NotificacionResponseDTO n : notificaciones) {
            if (n.getReferenciaId() == null) continue;
            Map<Integer, String> origen = switch (n.getTipo()) {
                case like, comentario -> publicaciones;
                case nueva_quedada, inscripcion_quedada, quedada_cancelada -> quedadas;
                case mensaje -> conversaciones;
                case valoracion_ruta -> rutas;
                default -> Map.of();
            };
            n.setReferenciaUuid(origen.get(n.getReferenciaId()));
        }
    }

    private static List<Integer> ids(List<NotificacionResponseDTO> notificaciones, TipoNotificacion... tipos) {
        List<TipoNotificacion> lista = List.of(tipos);
        return notificaciones.stream()
                .filter(n -> lista.contains(n.getTipo()))
                .map(NotificacionResponseDTO::getReferenciaId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static Map<Integer, String> uuids(List<Integer> ids, Function<Collection<Integer>, Map<Integer, String>> consulta) {
        return ids.isEmpty() ? Map.of() : consulta.apply(ids);
    }
}
