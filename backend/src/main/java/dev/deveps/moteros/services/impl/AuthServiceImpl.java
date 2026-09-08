package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.CambioPasswordDTO;
import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.JwtUtil;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final MotoRepository motoRepository;
    private final RutaRepository rutaRepository;
    private final AmistadRepository amistadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    public LoginResponseDTO registro(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new DuplicateResourceException("El nombre de usuario ya esta en uso: " + dto.getNombreUsuario());
        }

        Usuario usuario = Usuario.builder()
                .nombreUsuario(dto.getNombreUsuario())
                .nombreCompleto(dto.getNombreCompleto())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .ciudad(dto.getCiudad())
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return construirRespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository
                .findByEmailOrNombreUsuario(dto.getIdentificador(), dto.getIdentificador())
                .orElseThrow(() -> new BadRequestException("Credenciales invalidas"));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BadRequestException("La cuenta esta desactivada");
        }
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new BadRequestException("Credenciales invalidas");
        }

        return construirRespuesta(usuario);
    }

    @Override
    public void cambiarPassword(CambioPasswordDTO dto) {
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BadRequestException("La contrasena actual es incorrecta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }

    private LoginResponseDTO construirRespuesta(Usuario usuario) {
        String token = jwtUtil.generateToken(usuario.getEmail());
        long numMotos = motoRepository.countByUsuarioId(usuario.getId());
        long numRutas = rutaRepository.countByCreadorId(usuario.getId());
        long numAmigos = amistadRepository.countAmigosAceptados(usuario.getId());
        return LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .usuario(mapper.usuarioResponse(usuario, numMotos, numRutas, numAmigos))
                .build();
    }
}
