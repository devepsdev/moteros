package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.DenunciaRequestDTO;
import dev.deveps.moteros.dto.DenunciaResponseDTO;
import dev.deveps.moteros.dto.ResolverDenunciaDTO;
import dev.deveps.moteros.entities.Comentario;
import dev.deveps.moteros.entities.Denuncia;
import dev.deveps.moteros.entities.Mensaje;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Quedada;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.exceptions.TooManyRequestsException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ComentarioRepository;
import dev.deveps.moteros.repositories.DenunciaRepository;
import dev.deveps.moteros.repositories.MensajeRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AdminService;
import dev.deveps.moteros.services.AlmacenamientoService;
import dev.deveps.moteros.services.DenunciaService;
import dev.deveps.moteros.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DenunciaServiceImpl implements DenunciaService {

    /** Denuncias por usuario en 24 horas: de sobra para un uso normal, corta el abuso. */
    static final int MAX_DENUNCIAS_DIA = 20;

    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PublicacionRepository publicacionRepository;
    private final ComentarioRepository comentarioRepository;
    private final MensajeRepository mensajeRepository;
    private final RutaRepository rutaRepository;
    private final QuedadaRepository quedadaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final AdminService adminService;
    private final AlmacenamientoService almacenamientoService;
    private final EmailService emailService;
    private final EntityDtoMapper mapper;

    /** Lo denunciado, resuelto al crear la denuncia. */
    private record Objetivo(Usuario autor, String contenido, String imagenUrl) {
    }

    // ===================== APP =====================

    @Override
    public void denunciar(DenunciaRequestDTO dto) {
        Usuario yo = usuarioAutenticado.obtenerUsuarioActual();
        String referencia = dto.getReferenciaUuid().trim();
        Objetivo objetivo = resolverObjetivo(dto.getTipo(), referencia, yo);

        if (objetivo.autor() != null && objetivo.autor().getId().equals(yo.getId())) {
            throw new BadRequestException("No puedes denunciar tu propio contenido");
        }
        if (denunciaRepository.existsByDenuncianteIdAndTipoAndReferenciaUuidAndEstado(
                yo.getId(), dto.getTipo(), referencia, EstadoDenuncia.pendiente)) {
            return;
        }
        if (denunciaRepository.countByDenuncianteIdAndFechaCreacionAfter(
                yo.getId(), LocalDateTime.now().minusHours(24)) >= MAX_DENUNCIAS_DIA) {
            throw new TooManyRequestsException("Has enviado muchas denuncias hoy. Vuelve a intentarlo mañana.", 3600);
        }

        // Solo se avisa por correo de la primera denuncia pendiente sobre un contenido.
        boolean primera = !denunciaRepository.existsByTipoAndReferenciaUuidAndEstado(
                dto.getTipo(), referencia, EstadoDenuncia.pendiente);

        String descripcion = dto.getDescripcion() == null || dto.getDescripcion().isBlank() ? null : dto.getDescripcion().trim();
        denunciaRepository.save(Denuncia.builder()
                .denunciante(yo)
                .denunciado(objetivo.autor())
                .tipo(dto.getTipo())
                .referenciaUuid(referencia)
                .motivo(dto.getMotivo())
                .descripcion(descripcion)
                .contenido(objetivo.contenido())
                .imagenUrl(objetivo.imagenUrl())
                .estado(EstadoDenuncia.pendiente)
                .build());

        if (primera) {
            avisarAdministradores(dto.getTipo().name(), dto.getMotivo().name());
        }
    }

    // ===================== PANEL =====================

    @Override
    @Transactional(readOnly = true)
    public Page<DenunciaResponseDTO> listar(EstadoDenuncia estado, Pageable pageable) {
        Page<Denuncia> pagina;
        if (estado == null) {
            pagina = denunciaRepository.findAllByOrderByFechaCreacionDesc(pageable);
        } else if (estado == EstadoDenuncia.pendiente) {
            pagina = denunciaRepository.findByEstadoOrderByFechaCreacionAsc(estado, pageable);
        } else {
            pagina = denunciaRepository.findByEstadoOrderByFechaResolucionDesc(estado, pageable);
        }
        return pagina.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DenunciaResponseDTO obtener(String uuid) {
        return toResponse(buscar(uuid));
    }

    @Override
    public DenunciaResponseDTO resolver(String uuid, ResolverDenunciaDTO dto) {
        Denuncia denuncia = buscar(uuid);
        if (denuncia.getEstado() != EstadoDenuncia.pendiente) {
            throw new BadRequestException("La denuncia ya esta " + denuncia.getEstado());
        }
        Usuario admin = usuarioAutenticado.obtenerUsuarioActual();
        Usuario denunciado = denuncia.getDenunciado();

        if (dto.isDarDeBaja()) {
            if (denunciado == null) {
                throw new BadRequestException("El autor ya no tiene cuenta");
            }
            adminService.cambiarActivo(denunciado.getUuid(), false);
        }
        boolean eliminado = dto.isEliminarContenido() && eliminarContenido(denuncia);

        boolean medidas = dto.isDarDeBaja() || eliminado;
        String nota = dto.getNota() == null || dto.getNota().isBlank() ? null : dto.getNota().trim();
        LocalDateTime ahora = LocalDateTime.now();

        // Las demas denuncias pendientes sobre lo mismo se cierran con la misma decision.
        List<Denuncia> cerradas = Stream.concat(Stream.of(denuncia),
                        denunciaRepository.findByTipoAndReferenciaUuidAndEstado(
                                        denuncia.getTipo(), denuncia.getReferenciaUuid(), EstadoDenuncia.pendiente)
                                .stream().filter(d -> !d.getId().equals(denuncia.getId())))
                .collect(Collectors.toList());
        for (Denuncia d : cerradas) {
            d.setEstado(medidas ? EstadoDenuncia.resuelta : EstadoDenuncia.descartada);
            d.setContenidoEliminado(eliminado);
            d.setUsuarioDadoDeBaja(dto.isDarDeBaja());
            d.setNotaResolucion(nota);
            d.setResueltaPor(admin);
            d.setFechaResolucion(ahora);
        }
        denunciaRepository.saveAll(cerradas);
        return toResponse(denuncia);
    }

    // ===================== PRIVADOS =====================

    private Denuncia buscar(String uuid) {
        return denunciaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Denuncia no encontrada: " + uuid));
    }

    private Objetivo resolverObjetivo(TipoDenuncia tipo, String uuid, Usuario yo) {
        return switch (tipo) {
            case usuario -> {
                Usuario u = usuarioRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                yield new Objetivo(u, unir("@" + u.getNombreUsuario() + " · " + u.getNombreCompleto(), u.getBiografia()),
                        u.getFotoPerfilUrl());
            }
            case publicacion -> {
                Publicacion p = publicacionRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                yield new Objetivo(p.getUsuario(), p.getContenido(), p.getImagenUrl());
            }
            case comentario -> {
                Comentario c = comentarioRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                yield new Objetivo(c.getUsuario(), c.getContenido(), null);
            }
            case mensaje -> {
                Mensaje m = mensajeRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                // Solo se pueden denunciar los mensajes de una conversacion propia.
                Integer u1 = m.getConversacion().getUsuario1().getId();
                Integer u2 = m.getConversacion().getUsuario2().getId();
                if (!yo.getId().equals(u1) && !yo.getId().equals(u2)) {
                    throw noEncontrado(tipo);
                }
                yield new Objetivo(m.getRemitente(), m.getContenido(), null);
            }
            case ruta -> {
                Ruta r = rutaRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                yield new Objetivo(r.getCreador(), unir(r.getNombre(), r.getDescripcion()), null);
            }
            case quedada -> {
                Quedada q = quedadaRepository.findByUuid(uuid).orElseThrow(() -> noEncontrado(tipo));
                yield new Objetivo(q.getOrganizador(),
                        unir(q.getTitulo(), "Punto de encuentro: " + q.getPuntoEncuentro(), q.getDescripcion()), null);
            }
        };
    }

    /** Borra lo denunciado. Devuelve false si ya no existia. */
    private boolean eliminarContenido(Denuncia d) {
        String uuid = d.getReferenciaUuid();
        return switch (d.getTipo()) {
            case usuario -> usuarioRepository.findByUuid(uuid).map(u -> {
                // Un perfil no se borra: se le quitan la foto y la biografia.
                String foto = u.getFotoPerfilUrl();
                u.setFotoPerfilUrl(null);
                u.setBiografia(null);
                usuarioRepository.save(u);
                borrarArchivoAlConfirmar(foto);
                return true;
            }).orElse(false);
            case publicacion -> publicacionRepository.findByUuid(uuid).map(p -> {
                publicacionRepository.delete(p);
                borrarArchivoAlConfirmar(p.getImagenUrl());
                return true;
            }).orElse(false);
            case comentario -> comentarioRepository.findByUuid(uuid).map(c -> {
                comentarioRepository.delete(c);
                return true;
            }).orElse(false);
            case mensaje -> mensajeRepository.findByUuid(uuid).map(m -> {
                mensajeRepository.delete(m);
                return true;
            }).orElse(false);
            case ruta -> rutaRepository.findByUuid(uuid).map(r -> {
                rutaRepository.delete(r);
                return true;
            }).orElse(false);
            case quedada -> quedadaRepository.findByUuid(uuid).map(q -> {
                quedadaRepository.delete(q);
                return true;
            }).orElse(false);
        };
    }

    private boolean contenidoExiste(Denuncia d) {
        String uuid = d.getReferenciaUuid();
        return switch (d.getTipo()) {
            case usuario -> usuarioRepository.findByUuid(uuid).isPresent();
            case publicacion -> publicacionRepository.findByUuid(uuid).isPresent();
            case comentario -> comentarioRepository.findByUuid(uuid).isPresent();
            case mensaje -> mensajeRepository.findByUuid(uuid).isPresent();
            case ruta -> rutaRepository.findByUuid(uuid).isPresent();
            case quedada -> quedadaRepository.findByUuid(uuid).isPresent();
        };
    }

    /** El archivo solo se borra si la transaccion confirma, como al eliminar una cuenta. */
    private void borrarArchivoAlConfirmar(String url) {
        if (url == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    almacenamientoService.eliminarPorUrl(url);
                }
            });
        } else {
            almacenamientoService.eliminarPorUrl(url);
        }
    }

    /** Correo a los administradores, en segundo plano: un fallo del SMTP no afecta a la denuncia. */
    private void avisarAdministradores(String tipo, String motivo) {
        List<String> correos = usuarioRepository.findByRolAndActivoTrue(RolUsuario.admin).stream()
                .map(Usuario::getEmail)
                .toList();
        if (correos.isEmpty()) {
            return;
        }
        Runnable enviar = () -> CompletableFuture.runAsync(() -> correos.forEach(para -> {
            try {
                emailService.avisarDenuncia(para, tipo, motivo);
            } catch (RuntimeException e) {
                log.warn("No se ha podido avisar de una denuncia a un administrador: {}", e.getMessage());
            }
        }));
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

    private static ResourceNotFoundException noEncontrado(TipoDenuncia tipo) {
        return new ResourceNotFoundException("No se ha encontrado el contenido denunciado (" + tipo + ")");
    }

    private static String unir(String... partes) {
        return Stream.of(partes).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining("\n\n"));
    }

    private DenunciaResponseDTO toResponse(Denuncia d) {
        Usuario denunciado = d.getDenunciado();
        return DenunciaResponseDTO.builder()
                .uuid(d.getUuid())
                .denunciante(mapper.usuarioSummary(d.getDenunciante()))
                .denunciado(mapper.usuarioSummary(denunciado))
                .denunciadoActivo(denunciado != null ? denunciado.getActivo() : null)
                .denunciasContraDenunciado(denunciado != null ? denunciaRepository.countByDenunciadoId(denunciado.getId()) : 0)
                .tipo(d.getTipo())
                .referenciaUuid(d.getReferenciaUuid())
                .contenidoExiste(contenidoExiste(d))
                .motivo(d.getMotivo())
                .descripcion(d.getDescripcion())
                .contenido(d.getContenido())
                .imagenUrl(d.getImagenUrl())
                .estado(d.getEstado())
                .contenidoEliminado(d.isContenidoEliminado())
                .usuarioDadoDeBaja(d.isUsuarioDadoDeBaja())
                .notaResolucion(d.getNotaResolucion())
                .resueltaPor(mapper.usuarioSummary(d.getResueltaPor()))
                .fechaCreacion(d.getFechaCreacion())
                .fechaResolucion(d.getFechaResolucion())
                .build();
    }
}
