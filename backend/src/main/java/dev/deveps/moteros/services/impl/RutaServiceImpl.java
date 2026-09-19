package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.PuntoRutaRequestDTO;
import dev.deveps.moteros.dto.PuntoRutaResponseDTO;
import dev.deveps.moteros.dto.RutaFilterDTO;
import dev.deveps.moteros.dto.RutaRequestDTO;
import dev.deveps.moteros.dto.RutaResponseDTO;
import dev.deveps.moteros.dto.RutaSummaryDTO;
import dev.deveps.moteros.dto.ValoracionRutaRequestDTO;
import dev.deveps.moteros.dto.ValoracionRutaResponseDTO;
import dev.deveps.moteros.entities.PuntoRuta;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.ValoracionRuta;
import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.PuntoRutaRepository;
import dev.deveps.moteros.services.TrazadoService;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.ValoracionRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import dev.deveps.moteros.services.RutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final PuntoRutaRepository puntoRutaRepository;
    private final TrazadoService trazadoService;
    private final ValoracionRutaRepository valoracionRutaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final NotificacionService notificacionService;
    private final EntityDtoMapper mapper;

    // ===================== CONSULTAS =====================

    @Override
    @Transactional(readOnly = true)
    public Page<RutaSummaryDTO> buscar(String texto, Pageable pageable) {
        return rutaRepository.buscarPorTexto(texto, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RutaSummaryDTO> filtrar(RutaFilterDTO f, Pageable pageable) {
        Page<Ruta> pagina;
        if (f.hasGeoFilter()) {
            // La query nativa no admite ORDER BY por nombre de propiedad JPA: se pagina sin sort.
            Pageable sinSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            pagina = rutaRepository.buscarCercanas(
                    f.getLatitud().doubleValue(), f.getLongitud().doubleValue(), f.getRadioKm(), sinSort);
        } else {
            pagina = rutaRepository.filtrar(
                    limpiar(f.getNombre()),
                    f.getDificultad(),
                    f.getTipoTerreno(),
                    f.getDistanciaMinKm(),
                    f.getDistanciaMaxKm(),
                    f.getDuracionMaxMin(),
                    limpiar(f.getCreadorUuid()),
                    pageable);
        }
        return pagina.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RutaSummaryDTO> listarPorCreador(String creadorUuid, Pageable pageable) {
        return rutaRepository.findByCreadorUuid(creadorUuid, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RutaResponseDTO obtenerPorUuid(String uuid) {
        Ruta ruta = buscar(uuid);
        List<PuntoRutaResponseDTO> track = puntoRutaRepository
                .findByRutaIdOrderByOrdenAsc(ruta.getId()).stream()
                .map(mapper::puntoRutaResponse)
                .toList();
        return mapper.rutaResponse(ruta, media(ruta.getId()),
                valoracionRutaRepository.countByRutaId(ruta.getId()), track);
    }

    // ===================== ALTA / EDICION =====================

    @Override
    public RutaResponseDTO crear(RutaRequestDTO dto) {
        Usuario creador = usuarioAutenticado.obtenerUsuarioActual();
        Ruta ruta = Ruta.builder()
                .creador(creador)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .puntoInicio(dto.getPuntoInicio())
                .latitudInicio(dto.getLatitudInicio())
                .longitudInicio(dto.getLongitudInicio())
                .puntoFin(dto.getPuntoFin())
                .latitudFin(dto.getLatitudFin())
                .longitudFin(dto.getLongitudFin())
                .distanciaKm(dto.getDistanciaKm())
                .duracionEstimadaMin(dto.getDuracionEstimadaMin())
                .dificultad(dto.getDificultad() != null ? dto.getDificultad() : Dificultad.moderada)
                .tipoTerreno(dto.getTipoTerreno() != null ? dto.getTipoTerreno() : TipoTerreno.asfalto)
                .build();
        Ruta guardada = rutaRepository.save(ruta);

        if (dto.getPuntos() != null && !dto.getPuntos().isEmpty()) {
            guardarPuntos(guardada, dto.getPuntos());
        }
        aplicarTrazado(guardada);
        return obtenerPorUuid(guardada.getUuid());
    }

    @Override
    public RutaResponseDTO actualizar(String uuid, RutaRequestDTO dto) {
        Ruta ruta = buscar(uuid);
        exigirCreador(ruta);

        ruta.setNombre(dto.getNombre());
        ruta.setDescripcion(dto.getDescripcion());
        ruta.setPuntoInicio(dto.getPuntoInicio());
        ruta.setLatitudInicio(dto.getLatitudInicio());
        ruta.setLongitudInicio(dto.getLongitudInicio());
        ruta.setPuntoFin(dto.getPuntoFin());
        ruta.setLatitudFin(dto.getLatitudFin());
        ruta.setLongitudFin(dto.getLongitudFin());
        ruta.setDistanciaKm(dto.getDistanciaKm());
        ruta.setDuracionEstimadaMin(dto.getDuracionEstimadaMin());
        if (dto.getDificultad() != null) {
            ruta.setDificultad(dto.getDificultad());
        }
        if (dto.getTipoTerreno() != null) {
            ruta.setTipoTerreno(dto.getTipoTerreno());
        }
        rutaRepository.save(ruta);

        if (dto.getPuntos() != null) {
            reemplazarTrackInterno(ruta, dto.getPuntos());
        }
        aplicarTrazado(ruta);
        return obtenerPorUuid(ruta.getUuid());
    }

    @Override
    public void eliminar(String uuid) {
        Ruta ruta = buscar(uuid);
        exigirCreador(ruta);
        rutaRepository.delete(ruta);
    }

    // ===================== TRACK =====================

    @Override
    @Transactional(readOnly = true)
    public List<PuntoRutaResponseDTO> obtenerTrack(String rutaUuid) {
        buscar(rutaUuid);
        return puntoRutaRepository.findByRutaUuidOrderByOrdenAsc(rutaUuid).stream()
                .map(mapper::puntoRutaResponse)
                .toList();
    }

    @Override
    public List<PuntoRutaResponseDTO> reemplazarTrack(String rutaUuid, List<PuntoRutaRequestDTO> puntos) {
        Ruta ruta = buscar(rutaUuid);
        exigirCreador(ruta);
        reemplazarTrackInterno(ruta, puntos != null ? puntos : List.of());
        aplicarTrazado(ruta);
        return obtenerTrack(rutaUuid);
    }

    // ===================== TRAZADO POR CARRETERA =====================

    @Override
    @Transactional(readOnly = true)
    public Optional<TrazadoService.Trazado> vistaPreviaTrazado(List<double[]> puntos) {
        return trazadoService.calcular(puntos);
    }

    @Override
    public boolean recalcularTrazado(Integer rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + rutaId));
        aplicarTrazado(ruta);
        return ruta.getTrazado() != null;
    }

    // ===================== VALORACIONES =====================

    @Override
    @Transactional(readOnly = true)
    public Page<ValoracionRutaResponseDTO> listarValoraciones(String rutaUuid, Pageable pageable) {
        buscar(rutaUuid);
        return valoracionRutaRepository
                .findByRutaUuidOrderByFechaDesc(rutaUuid, pageable)
                .map(mapper::valoracionResponse);
    }

    @Override
    public ValoracionRutaResponseDTO valorar(String rutaUuid, ValoracionRutaRequestDTO dto) {
        Ruta ruta = buscar(rutaUuid);
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();

        ValoracionRuta valoracion = valoracionRutaRepository
                .findByRutaIdAndUsuarioId(ruta.getId(), usuario.getId())
                .orElseGet(() -> ValoracionRuta.builder().ruta(ruta).usuario(usuario).build());
        boolean esNueva = valoracion.getId() == null;

        valoracion.setPuntuacion(dto.getPuntuacion().byteValue());
        valoracion.setComentario(dto.getComentario());
        ValoracionRuta guardada = valoracionRutaRepository.save(valoracion);

        if (esNueva) {
            notificacionService.notificar(ruta.getCreador(), TipoNotificacion.valoracion_ruta,
                    ruta.getId(), usuario, usuario.getNombreCompleto() + " ha valorado tu ruta \""
                            + ruta.getNombre() + "\" con " + dto.getPuntuacion() + " estrellas.");
        }

        return mapper.valoracionResponse(guardada);
    }

    @Override
    public void eliminarMiValoracion(String rutaUuid) {
        Ruta ruta = buscar(rutaUuid);
        Integer usuarioId = usuarioAutenticado.obtenerIdUsuarioActual();
        ValoracionRuta valoracion = valoracionRutaRepository
                .findByRutaIdAndUsuarioId(ruta.getId(), usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No has valorado esta ruta"));
        valoracionRutaRepository.delete(valoracion);
    }

    // ===================== PRIVADOS =====================

    private Ruta buscar(String uuid) {
        return rutaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + uuid));
    }

    /** Solo el creador o un administrador (que mantiene el catalogo desde el panel) pueden modificarla. */
    private void exigirCreador(Ruta ruta) {
        Usuario actual = usuarioAutenticado.obtenerUsuarioActual();
        if (actual.getRol() != RolUsuario.admin && !ruta.getCreador().getId().equals(actual.getId())) {
            throw new BadRequestException("No puedes modificar una ruta que no has creado");
        }
    }

    private RutaSummaryDTO toSummary(Ruta ruta) {
        return mapper.rutaSummary(ruta, media(ruta.getId()),
                valoracionRutaRepository.countByRutaId(ruta.getId()));
    }

    private Double media(Integer rutaId) {
        Double media = valoracionRutaRepository.mediaPuntuacion(rutaId);
        return media == null ? null : Math.round(media * 100.0) / 100.0;
    }

    /**
     * Calcula y guarda el recorrido por carretera de la ruta a partir de sus puntos. Si el
     * servicio no responde, el trazado queda sin calcular (null) y lo reintenta
     * {@code TrazadoRutasJob}. Con el recorrido calculado, la distancia pasa a ser la real por
     * carretera y, si no se indico, se estima la duracion.
     */
    private void aplicarTrazado(Ruta ruta) {
        List<double[]> puntos = puntoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId()).stream()
                .map(p -> new double[]{p.getLatitud().doubleValue(), p.getLongitud().doubleValue()})
                .toList();
        String trazado = null;
        if (puntos.size() >= TrazadoService.MIN_PUNTOS && puntos.size() <= TrazadoService.MAX_PUNTOS) {
            Optional<TrazadoService.Trazado> calculado = trazadoService.calcular(puntos);
            if (calculado.isPresent()) {
                TrazadoService.Trazado t = calculado.get();
                trazado = t.polilinea();
                if (t.encontrado()) {
                    ruta.setDistanciaKm(java.math.BigDecimal.valueOf(t.distanciaKm()));
                    if (ruta.getDuracionEstimadaMin() == null && t.duracionMin() > 0) {
                        ruta.setDuracionEstimadaMin(t.duracionMin());
                    }
                }
            }
        }
        ruta.setTrazado(trazado);
        rutaRepository.save(ruta);
    }

    private void reemplazarTrackInterno(Ruta ruta, List<PuntoRutaRequestDTO> puntos) {
        puntoRutaRepository.deleteByRutaId(ruta.getId());
        puntoRutaRepository.flush();
        if (!puntos.isEmpty()) {
            guardarPuntos(ruta, puntos);
        }
    }

    private void guardarPuntos(Ruta ruta, List<PuntoRutaRequestDTO> puntos) {
        List<PuntoRuta> entidades = puntos.stream()
                .map(p -> PuntoRuta.builder()
                        .ruta(ruta)
                        .orden(p.getOrden())
                        .latitud(p.getLatitud())
                        .longitud(p.getLongitud())
                        .altitudM(p.getAltitudM())
                        .nombrePunto(p.getNombrePunto())
                        .via(p.isVia())
                        .build())
                .toList();
        puntoRutaRepository.saveAll(entidades);
    }

    private static String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
