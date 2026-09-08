package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.UsuarioRequestDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MotoRepository motoRepository;
    private final RutaRepository rutaRepository;
    private final AmistadRepository amistadRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPerfilActual() {
        return conContadores(usuarioAutenticado.obtenerUsuarioActual());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorUuid(String uuid) {
        Usuario usuario = usuarioRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + uuid));
        return conContadores(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioSummaryDTO> buscar(String texto, Pageable pageable) {
        return usuarioRepository.buscarPorTexto(texto, pageable).map(mapper::usuarioSummary);
    }

    @Override
    public UsuarioResponseDTO actualizarPerfil(UsuarioRequestDTO dto) {
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();

        if (!usuario.getNombreUsuario().equals(dto.getNombreUsuario())
                && usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new DuplicateResourceException("El nombre de usuario ya esta en uso: " + dto.getNombreUsuario());
        }

        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCiudad(dto.getCiudad());
        usuario.setBiografia(dto.getBiografia());
        usuario.setFotoPerfilUrl(dto.getFotoPerfilUrl());

        return conContadores(usuarioRepository.save(usuario));
    }

    private UsuarioResponseDTO conContadores(Usuario usuario) {
        long numMotos = motoRepository.countByUsuarioId(usuario.getId());
        long numRutas = rutaRepository.countByCreadorId(usuario.getId());
        long numAmigos = amistadRepository.countAmigosAceptados(usuario.getId());
        return mapper.usuarioResponse(usuario, numMotos, numRutas, numAmigos);
    }
}
