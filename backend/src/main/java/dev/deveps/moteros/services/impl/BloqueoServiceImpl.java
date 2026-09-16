package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.entities.Bloqueo;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.BloqueoRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.BloqueoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BloqueoServiceImpl implements BloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final AmistadRepository amistadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    public void bloquear(String usuarioUuid) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        Usuario otro = usuarioPorUuid(usuarioUuid);
        if (yo.getId().equals(otro.getId())) {
            throw new BadRequestException("No puedes bloquearte a ti mismo");
        }
        if (bloqueoRepository.existsByBloqueadorIdAndBloqueadoId(yo.getId(), otro.getId())) {
            return;
        }
        amistadRepository.findRelacion(yo.getId(), otro.getId()).ifPresent(amistadRepository::delete);
        bloqueoRepository.save(Bloqueo.builder().bloqueador(yo).bloqueado(otro).build());
    }

    @Override
    public void desbloquear(String usuarioUuid) {
        Integer yoId = usuarioAutenticado.obtenerIdUsuarioActual();
        Usuario otro = usuarioPorUuid(usuarioUuid);
        bloqueoRepository.findByBloqueadorIdAndBloqueadoId(yoId, otro.getId())
                .ifPresent(bloqueoRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioSummaryDTO> misBloqueados(Pageable pageable) {
        return bloqueoRepository.findBloqueados(usuarioAutenticado.obtenerIdUsuarioActual(), pageable)
                .map(mapper::usuarioSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean heBloqueado(String usuarioUuid) {
        Usuario otro = usuarioPorUuid(usuarioUuid);
        return bloqueoRepository.existsByBloqueadorIdAndBloqueadoId(
                usuarioAutenticado.obtenerIdUsuarioActual(), otro.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirSinBloqueo(Integer otroUsuarioId) {
        // Se responde como si no existiera: quien ha sido bloqueado no debe poder deducirlo.
        if (bloqueoRepository.existeEntre(usuarioAutenticado.obtenerIdUsuarioActual(), otroUsuarioId)) {
            throw new ResourceNotFoundException("No disponible");
        }
    }

    private Usuario usuarioPorUuid(String uuid) {
        return usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + uuid));
    }
}
