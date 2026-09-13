package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.UsuarioRequestDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AlmacenamientoService;
import dev.deveps.moteros.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MotoRepository motoRepository;
    private final RutaRepository rutaRepository;
    private final AmistadRepository amistadRepository;
    private final PublicacionRepository publicacionRepository;
    private final AlmacenamientoService almacenamientoService;
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

    @Override
    public void eliminarCuentaActual() {
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();

        if (usuario.getRol() == RolUsuario.admin && usuarioRepository.countByRol(RolUsuario.admin) <= 1) {
            throw new BadRequestException(
                    "Eres el unico administrador. Asigna el rol de admin a otro usuario antes de eliminar tu cuenta");
        }

        // Se recogen las URLs antes de borrar: despues las filas ya no existen.
        List<String> imagenes = new ArrayList<>();
        if (usuario.getFotoPerfilUrl() != null) {
            imagenes.add(usuario.getFotoPerfilUrl());
        }
        imagenes.addAll(motoRepository.fotosDeUsuario(usuario.getId()));
        imagenes.addAll(publicacionRepository.imagenesDeUsuario(usuario.getId()));

        // El resto de datos del usuario los borra MySQL por ON DELETE CASCADE / SET NULL.
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();

        // Los archivos se borran solo si la transaccion confirma, para no perder
        // imagenes de una cuenta cuyo borrado se ha deshecho.
        Runnable borrarArchivos = () -> imagenes.forEach(almacenamientoService::eliminarPorUrl);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    borrarArchivos.run();
                }
            });
        } else {
            borrarArchivos.run();
        }
    }

    private UsuarioResponseDTO conContadores(Usuario usuario) {
        long numMotos = motoRepository.countByUsuarioId(usuario.getId());
        long numRutas = rutaRepository.countByCreadorId(usuario.getId());
        long numAmigos = amistadRepository.countAmigosAceptados(usuario.getId());
        return mapper.usuarioResponse(usuario, numMotos, numRutas, numAmigos);
    }
}
