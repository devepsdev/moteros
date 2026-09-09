package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.InscripcionQuedadaResponseDTO;
import dev.deveps.moteros.dto.QuedadaFilterDTO;
import dev.deveps.moteros.dto.QuedadaRequestDTO;
import dev.deveps.moteros.dto.QuedadaResponseDTO;
import dev.deveps.moteros.dto.QuedadaSummaryDTO;
import dev.deveps.moteros.entities.InscripcionQuedada;
import dev.deveps.moteros.entities.Quedada;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.InscripcionQuedadaRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.ValoracionRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import dev.deveps.moteros.services.QuedadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuedadaServiceImpl implements QuedadaService {

    private final QuedadaRepository quedadaRepository;
    private final InscripcionQuedadaRepository inscripcionRepository;
    private final RutaRepository rutaRepository;
    private final ValoracionRutaRepository valoracionRutaRepository;
    private final AmistadRepository amistadRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final NotificacionService notificacionService;
    private final EntityDtoMapper mapper;

    // ===================== CONSULTAS =====================

    @Override
    @Transactional(readOnly = true)
    public Page<QuedadaSummaryDTO> buscar(String texto, Pageable pageable) {
        return quedadaRepository.buscarPorTexto(texto, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuedadaSummaryDTO> filtrar(QuedadaFilterDTO f, Pageable pageable) {
        Page<Quedada> pagina;
        if (f.hasGeoFilter()) {
            // La query nativa no admite ORDER BY por nombre de propiedad JPA: se pagina sin sort.
            Pageable sinSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            pagina = quedadaRepository.buscarCercanas(
                    f.getLatitud().doubleValue(), f.getLongitud().doubleValue(), f.getRadioKm(), sinSort);
        } else {
            LocalDateTime ahora = Boolean.TRUE.equals(f.getSoloProximas()) ? LocalDateTime.now() : null;
            pagina = quedadaRepository.filtrar(
                    limpiar(f.getTitulo()),
                    f.getNivelRecomendado(),
                    f.getEstado(),
                    limpiar(f.getRutaUuid()),
                    limpiar(f.getOrganizadorUuid()),
                    f.getFechaDesde(),
                    f.getFechaHasta(),
                    ahora,
                    pageable);
        }
        return pagina.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuedadaSummaryDTO> listarPorOrganizador(String organizadorUuid, Pageable pageable) {
        return quedadaRepository.findByOrganizadorUuid(organizadorUuid, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuedadaSummaryDTO> misInscripciones(Pageable pageable) {
        String uuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        return inscripcionRepository.findByUsuarioUuid(uuid, pageable)
                .map(i -> toSummary(i.getQuedada()));
    }

    @Override
    @Transactional(readOnly = true)
    public QuedadaResponseDTO obtenerPorUuid(String uuid) {
        return detalle(buscar(uuid));
    }

    // ===================== ALTA / EDICION =====================

    @Override
    public QuedadaResponseDTO crear(QuedadaRequestDTO dto) {
        Usuario organizador = usuarioAutenticado.obtenerUsuarioActual();
        Quedada quedada = Quedada.builder()
                .organizador(organizador)
                .ruta(resolverRuta(dto.getRutaUuid()))
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .puntoEncuentro(dto.getPuntoEncuentro())
                .latitudEncuentro(dto.getLatitudEncuentro())
                .longitudEncuentro(dto.getLongitudEncuentro())
                .fechaHora(dto.getFechaHora())
                .maxParticipantes(dto.getMaxParticipantes() != null ? dto.getMaxParticipantes() : 20)
                .nivelRecomendado(dto.getNivelRecomendado() != null ? dto.getNivelRecomendado() : NivelRecomendado.cualquiera)
                .estado(EstadoQuedada.programada)
                .build();
        Quedada guardada = quedadaRepository.save(quedada);

        // Avisar a los amigos del organizador de la nueva quedada.
        amistadRepository.findAmigosAceptadosLista(organizador.getId()).forEach(amigo ->
                notificacionService.notificar(amigo, TipoNotificacion.nueva_quedada, guardada.getId(),
                        organizador, organizador.getNombreCompleto()
                                + " ha organizado una nueva quedada: \"" + guardada.getTitulo() + "\"."));

        return detalle(guardada);
    }

    @Override
    public QuedadaResponseDTO actualizar(String uuid, QuedadaRequestDTO dto) {
        Quedada quedada = buscar(uuid);
        exigirOrganizador(quedada);

        quedada.setRuta(resolverRuta(dto.getRutaUuid()));
        quedada.setTitulo(dto.getTitulo());
        quedada.setDescripcion(dto.getDescripcion());
        quedada.setPuntoEncuentro(dto.getPuntoEncuentro());
        quedada.setLatitudEncuentro(dto.getLatitudEncuentro());
        quedada.setLongitudEncuentro(dto.getLongitudEncuentro());
        quedada.setFechaHora(dto.getFechaHora());
        if (dto.getMaxParticipantes() != null) {
            quedada.setMaxParticipantes(dto.getMaxParticipantes());
        }
        if (dto.getNivelRecomendado() != null) {
            quedada.setNivelRecomendado(dto.getNivelRecomendado());
        }
        return detalle(quedadaRepository.save(quedada));
    }

    @Override
    public QuedadaResponseDTO cambiarEstado(String uuid, EstadoQuedada estado) {
        Quedada quedada = buscar(uuid);
        exigirOrganizador(quedada);
        boolean seCancela = estado == EstadoQuedada.cancelada && quedada.getEstado() != EstadoQuedada.cancelada;
        quedada.setEstado(estado);
        Quedada guardada = quedadaRepository.save(quedada);

        if (seCancela) {
            inscripcionRepository.findByQuedadaId(guardada.getId()).forEach(inscripcion ->
                    notificacionService.notificar(inscripcion.getUsuario(), TipoNotificacion.quedada_cancelada,
                            guardada.getId(), guardada.getOrganizador(),
                            "Se ha cancelado la quedada \"" + guardada.getTitulo() + "\"."));
        }
        return detalle(guardada);
    }

    @Override
    public void eliminar(String uuid) {
        Quedada quedada = buscar(uuid);
        exigirOrganizador(quedada);
        quedadaRepository.delete(quedada);
    }

    // ===================== INSCRIPCIONES =====================

    @Override
    public InscripcionQuedadaResponseDTO inscribirse(String quedadaUuid) {
        Quedada quedada = buscar(quedadaUuid);
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();

        if (quedada.getEstado() != EstadoQuedada.programada) {
            throw new BadRequestException("La quedada no admite inscripciones");
        }
        if (quedada.getOrganizador().getId().equals(usuario.getId())) {
            throw new BadRequestException("El organizador no necesita inscribirse a su propia quedada");
        }

        InscripcionQuedada inscripcion = inscripcionRepository
                .findByQuedadaIdAndUsuarioId(quedada.getId(), usuario.getId())
                .orElseGet(() -> InscripcionQuedada.builder().quedada(quedada).usuario(usuario).build());

        if (inscripcion.getId() != null && inscripcion.getEstado() != EstadoInscripcion.cancelado) {
            throw new BadRequestException("Ya estas inscrito en esta quedada");
        }

        long confirmados = inscripcionRepository
                .countByQuedadaIdAndEstado(quedada.getId(), EstadoInscripcion.confirmado);
        if (quedada.getMaxParticipantes() != null && confirmados >= quedada.getMaxParticipantes()) {
            throw new BadRequestException("La quedada esta completa");
        }

        inscripcion.setEstado(EstadoInscripcion.confirmado);
        InscripcionQuedada guardada = inscripcionRepository.save(inscripcion);

        notificacionService.notificar(quedada.getOrganizador(), TipoNotificacion.inscripcion_quedada,
                quedada.getId(), usuario,
                usuario.getNombreCompleto() + " se ha apuntado a tu quedada \"" + quedada.getTitulo() + "\".");

        return mapper.inscripcionResponse(guardada);
    }

    @Override
    public void cancelarInscripcion(String quedadaUuid) {
        String usuarioUuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        InscripcionQuedada inscripcion = inscripcionRepository
                .findByQuedadaUuidAndUsuarioUuid(quedadaUuid, usuarioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("No estas inscrito en esta quedada"));
        inscripcionRepository.delete(inscripcion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionQuedadaResponseDTO> listarInscritos(String quedadaUuid) {
        buscar(quedadaUuid);
        return inscripcionRepository.findByQuedadaUuid(quedadaUuid).stream()
                .map(mapper::inscripcionResponse)
                .toList();
    }

    @Override
    public InscripcionQuedadaResponseDTO cambiarEstadoInscripcion(String quedadaUuid, String usuarioUuid,
                                                                  EstadoInscripcion estado) {
        Quedada quedada = buscar(quedadaUuid);
        exigirOrganizador(quedada);
        InscripcionQuedada inscripcion = inscripcionRepository
                .findByQuedadaUuidAndUsuarioUuid(quedadaUuid, usuarioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada"));
        inscripcion.setEstado(estado);
        return mapper.inscripcionResponse(inscripcionRepository.save(inscripcion));
    }

    // ===================== PRIVADOS =====================

    private Quedada buscar(String uuid) {
        return quedadaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Quedada no encontrada: " + uuid));
    }

    private void exigirOrganizador(Quedada quedada) {
        if (!quedada.getOrganizador().getId().equals(usuarioAutenticado.obtenerIdUsuarioActual())) {
            throw new BadRequestException("Solo el organizador puede gestionar esta quedada");
        }
    }

    private Ruta resolverRuta(String rutaUuid) {
        if (rutaUuid == null || rutaUuid.isBlank()) {
            return null;
        }
        return rutaRepository.findByUuid(rutaUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + rutaUuid));
    }

    private QuedadaSummaryDTO toSummary(Quedada quedada) {
        long inscritos = inscripcionRepository.countByQuedadaId(quedada.getId());
        return mapper.quedadaSummary(quedada, inscritos);
    }

    private QuedadaResponseDTO detalle(Quedada quedada) {
        Integer quedadaId = quedada.getId();
        long numInscritos = inscripcionRepository.countByQuedadaId(quedadaId);
        long confirmados = inscripcionRepository.countByQuedadaIdAndEstado(quedadaId, EstadoInscripcion.confirmado);
        Integer plazasLibres = quedada.getMaxParticipantes() == null
                ? null
                : Math.max(0, quedada.getMaxParticipantes() - (int) confirmados);

        Integer usuarioId = usuarioAutenticado.obtenerIdUsuarioActual();
        EstadoInscripcion miEstado = inscripcionRepository
                .findByQuedadaIdAndUsuarioId(quedadaId, usuarioId)
                .map(InscripcionQuedada::getEstado)
                .orElse(null);

        List<InscripcionQuedadaResponseDTO> inscritos = inscripcionRepository.findByQuedadaId(quedadaId).stream()
                .map(mapper::inscripcionResponse)
                .toList();

        Double rutaMedia = null;
        Long rutaNumVal = null;
        if (quedada.getRuta() != null) {
            rutaMedia = valoracionRutaRepository.mediaPuntuacion(quedada.getRuta().getId());
            if (rutaMedia != null) {
                rutaMedia = Math.round(rutaMedia * 100.0) / 100.0;
            }
            rutaNumVal = valoracionRutaRepository.countByRutaId(quedada.getRuta().getId());
        }

        return mapper.quedadaResponse(quedada, numInscritos, plazasLibres, miEstado, inscritos, rutaMedia, rutaNumVal);
    }

    private static String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
