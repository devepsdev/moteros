package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.ConversacionResponseDTO;
import dev.deveps.moteros.dto.MensajeRequestDTO;
import dev.deveps.moteros.dto.MensajeResponseDTO;
import dev.deveps.moteros.entities.Conversacion;
import dev.deveps.moteros.entities.Mensaje;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ConversacionRepository;
import dev.deveps.moteros.repositories.MensajeRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.ChatService;
import dev.deveps.moteros.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final NotificacionService notificacionService;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversacionResponseDTO> misConversaciones(Pageable pageable) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        return conversacionRepository.findByParticipante(yoId, pageable)
                .map(c -> toResponse(c, yoId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversacionResponseDTO obtenerConversacion(String conversacionUuid) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Conversacion c = buscar(conversacionUuid);
        exigirParticipante(c, yoId);
        return toResponse(c, yoId);
    }

    @Override
    public Page<MensajeResponseDTO> listarMensajes(String conversacionUuid, Pageable pageable) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Conversacion c = buscar(conversacionUuid);
        exigirParticipante(c, yoId);
        mensajeRepository.marcarLeidos(c.getId(), yoId);
        return mensajeRepository
                .findByConversacionUuidOrderByFechaEnvioDesc(conversacionUuid, pageable)
                .map(m -> mapper.mensajeResponse(m, yoId));
    }

    @Override
    public MensajeResponseDTO enviarMensajeAUsuario(String usuarioUuid, MensajeRequestDTO dto) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        Usuario otro = usuarioRepository.findByUuid(usuarioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioUuid));
        if (yo.getId().equals(otro.getId())) {
            throw new BadRequestException("No puedes enviarte mensajes a ti mismo");
        }
        Conversacion c = obtenerOCrearConversacion(yo, otro);
        return guardarMensaje(c, yo, otro, dto.getContenido());
    }

    @Override
    public MensajeResponseDTO enviarMensajeEnConversacion(String conversacionUuid, MensajeRequestDTO dto) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        Conversacion c = buscar(conversacionUuid);
        exigirParticipante(c, yo.getId());
        Usuario otro = c.getUsuario1().getId().equals(yo.getId()) ? c.getUsuario2() : c.getUsuario1();
        return guardarMensaje(c, yo, otro, dto.getContenido());
    }

    @Override
    public void marcarLeidos(String conversacionUuid) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Conversacion c = buscar(conversacionUuid);
        exigirParticipante(c, yoId);
        mensajeRepository.marcarLeidos(c.getId(), yoId);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalNoLeidos() {
        return mensajeRepository.countNoLeidosTotal(usuarioAutenticado.obtenerIdUsuarioActual());
    }

    // ===================== PRIVADOS =====================

    private Conversacion buscar(String uuid) {
        return conversacionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion no encontrada: " + uuid));
    }

    private void exigirParticipante(Conversacion c, Integer usuarioId) {
        if (!c.getUsuario1().getId().equals(usuarioId) && !c.getUsuario2().getId().equals(usuarioId)) {
            throw new BadRequestException("No formas parte de esta conversacion");
        }
    }

    /** Devuelve la conversacion del par (creandola si no existe). Se guarda con usuario1Id &lt; usuario2Id. */
    private Conversacion obtenerOCrearConversacion(Usuario a, Usuario b) {
        Usuario menor = a.getId() < b.getId() ? a : b;
        Usuario mayor = a.getId() < b.getId() ? b : a;
        return conversacionRepository.findByUsuario1IdAndUsuario2Id(menor.getId(), mayor.getId())
                .orElseGet(() -> conversacionRepository.save(
                        Conversacion.builder().usuario1(menor).usuario2(mayor).build()));
    }

    private MensajeResponseDTO guardarMensaje(Conversacion c, Usuario remitente, Usuario destinatario, String contenido) {
        Mensaje mensaje = mensajeRepository.save(Mensaje.builder()
                .conversacion(c)
                .remitente(remitente)
                .contenido(contenido)
                .leido(false)
                .build());

        notificacionService.notificar(destinatario, TipoNotificacion.mensaje, c.getId(), remitente,
                remitente.getNombreCompleto() + " te ha enviado un mensaje.");

        return mapper.mensajeResponse(mensaje, remitente.getId());
    }

    private ConversacionResponseDTO toResponse(Conversacion c, Integer yoId) {
        Usuario interlocutor = c.getUsuario1().getId().equals(yoId) ? c.getUsuario2() : c.getUsuario1();
        MensajeResponseDTO ultimo = mensajeRepository
                .findTop1ByConversacionIdOrderByFechaEnvioDesc(c.getId())
                .map(m -> mapper.mensajeResponse(m, yoId))
                .orElse(null);
        long noLeidos = mensajeRepository
                .countByConversacionIdAndLeidoFalseAndRemitenteIdNot(c.getId(), yoId);
        return mapper.conversacionResponse(c, mapper.usuarioSummary(interlocutor), ultimo, noLeidos);
    }
}
