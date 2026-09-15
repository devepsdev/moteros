package dev.deveps.moteros.config;

import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea (o asciende a admin) el administrador indicado por variables de entorno al arrancar,
 * como en rastrix. Si ADMIN_EMAIL no esta configurado (desarrollo local), no hace nada.
 *
 * Si ya existe una cuenta con ese email, solo se asegura de que sea admin: la contrasena no
 * se toca, asi que ADMIN_PASSWORD puede borrarse del .env una vez creada la cuenta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.nombre-usuario:}")
    private String nombreUsuario;

    @Value("${app.admin.nombre-completo:}")
    private String nombreCompleto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String emailNormalizado = email.trim();

        usuarioRepository.findByEmail(emailNormalizado).ifPresentOrElse(
                usuario -> {
                    if (usuario.getRol() != RolUsuario.admin || !Boolean.TRUE.equals(usuario.getActivo())) {
                        usuario.setRol(RolUsuario.admin);
                        usuario.setActivo(true);
                        usuarioRepository.save(usuario);
                        log.info("Usuario {} ascendido a admin", emailNormalizado);
                    }
                },
                () -> crear(emailNormalizado)
        );
    }

    private void crear(String emailNormalizado) {
        if (password == null || password.length() < 8) {
            log.warn("ADMIN_EMAIL={} no existe y ADMIN_PASSWORD falta o tiene menos de 8 caracteres: no se crea el administrador",
                    emailNormalizado);
            return;
        }
        String usuarioElegido = nombreUsuario == null || nombreUsuario.isBlank() ? "admin" : nombreUsuario.trim();
        if (usuarioRepository.existsByNombreUsuario(usuarioElegido)) {
            log.warn("El nombre de usuario {} ya esta en uso por otra cuenta: no se crea el administrador", usuarioElegido);
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .nombreUsuario(usuarioElegido)
                .nombreCompleto(nombreCompleto == null || nombreCompleto.isBlank() ? "Administrador" : nombreCompleto.trim())
                .email(emailNormalizado)
                .passwordHash(passwordEncoder.encode(password))
                .activo(true)
                .rol(RolUsuario.admin)
                .build());
        log.info("Administrador {} ({}) creado", usuarioElegido, emailNormalizado);
    }
}
