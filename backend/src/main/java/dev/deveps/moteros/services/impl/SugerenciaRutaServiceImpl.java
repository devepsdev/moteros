package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.EnlaceTrackDTO;
import dev.deveps.moteros.dto.PuntoSugeridoDTO;
import dev.deveps.moteros.dto.SugerenciaRutaRequestDTO;
import dev.deveps.moteros.dto.SugerenciaRutaResponseDTO;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.SugerenciaRuta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.SugerenciaRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.SugerenciaRutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SugerenciaRutaServiceImpl implements SugerenciaRutaService {

    /**
     * Tope de sugerencias pendientes por cuenta. Es alto porque una pasada del scraper puede
     * traer muchas rutas, pero existe para que un fallo del bot no inunde la bandeja.
     */
    static final int MAX_PENDIENTES = 300;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<PuntoSugeridoDTO>> LISTA_PUNTOS = new TypeReference<>() { };
    private static final TypeReference<List<EnlaceTrackDTO>> LISTA_ENLACES = new TypeReference<>() { };

    private final SugerenciaRutaRepository sugerenciaRepository;
    private final RutaRepository rutaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    public SugerenciaRutaResponseDTO crear(SugerenciaRutaRequestDTO dto) {
        Usuario autor = usuarioAutenticado.obtenerUsuarioActual();
        if (autor.getRol() != RolUsuario.scraper && autor.getRol() != RolUsuario.admin) {
            throw new BadRequestException("Solo la cuenta del scraper puede enviar sugerencias de rutas");
        }
        if (sugerenciaRepository.countByUsuarioIdAndEstado(autor.getId(), EstadoSugerencia.pendiente) >= MAX_PENDIENTES) {
            throw new BadRequestException("Ya hay " + MAX_PENDIENTES
                    + " sugerencias pendientes. Revisalas antes de la proxima pasada del scraper.");
        }

        String nombre = normalizar(dto.getNombre());
        String puntoInicio = normalizar(dto.getPuntoInicio());
        // El scraper vuelve a leer las mismas paginas cada semana: solo debe proponer lo que no
        // esta ya en el catalogo ni se propuso antes, aunque se rechazara.
        if (rutaRepository.existeEnCatalogo(nombre, puntoInicio)) {
            throw new DuplicateResourceException("Esa ruta ya esta en el catalogo: " + nombre + " (" + puntoInicio + ")");
        }
        if (sugerenciaRepository.existeSugerida(nombre, puntoInicio)) {
            throw new DuplicateResourceException("Esa ruta ya se sugirio antes: " + nombre + " (" + puntoInicio + ")");
        }

        SugerenciaRuta sugerencia = SugerenciaRuta.builder()
                .usuario(autor)
                .urlFuente(dto.getUrlFuente().trim())
                .nombre(nombre)
                .descripcion(dto.getDescripcion())
                .puntoInicio(puntoInicio)
                .puntoFin(normalizar(dto.getPuntoFin()))
                .distanciaKm(dto.getDistanciaKm())
                .duracionEstimadaMin(dto.getDuracionEstimadaMin())
                .dificultad(dto.getDificultad())
                .tipoTerreno(dto.getTipoTerreno())
                .puntosJson(dto.getPuntos() == null || dto.getPuntos().isEmpty() ? null : JSON.writeValueAsString(dto.getPuntos()))
                .enlacesTrackJson(dto.getEnlacesTrack() == null || dto.getEnlacesTrack().isEmpty() ? null : JSON.writeValueAsString(dto.getEnlacesTrack()))
                .estado(EstadoSugerencia.pendiente)
                .build();
        return toResponse(sugerenciaRepository.save(sugerencia));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SugerenciaRutaResponseDTO> listar(EstadoSugerencia estado, Pageable pageable) {
        Page<SugerenciaRuta> pagina = estado == null
                ? sugerenciaRepository.findAllByOrderByFechaCreacionDesc(pageable)
                : sugerenciaRepository.findByEstadoOrderByFechaCreacionDesc(estado, pageable);
        return pagina.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SugerenciaRutaResponseDTO obtener(String uuid) {
        return toResponse(buscar(uuid));
    }

    @Override
    public SugerenciaRutaResponseDTO aprobar(String uuid, String rutaUuid) {
        SugerenciaRuta sugerencia = buscarPendiente(uuid);
        Ruta ruta = rutaRepository.findByUuid(rutaUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + rutaUuid));
        sugerencia.setEstado(EstadoSugerencia.aprobada);
        sugerencia.setRuta(ruta);
        sugerencia.setMotivoRechazo(null);
        return toResponse(sugerenciaRepository.save(sugerencia));
    }

    @Override
    public SugerenciaRutaResponseDTO rechazar(String uuid, String motivo) {
        SugerenciaRuta sugerencia = buscarPendiente(uuid);
        sugerencia.setEstado(EstadoSugerencia.rechazada);
        sugerencia.setMotivoRechazo(motivo == null || motivo.isBlank() ? null : motivo.trim());
        return toResponse(sugerenciaRepository.save(sugerencia));
    }

    // ===================== PRIVADOS =====================

    private SugerenciaRuta buscar(String uuid) {
        return sugerenciaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Sugerencia no encontrada: " + uuid));
    }

    /** Aprobar o rechazar solo tiene sentido una vez: una sugerencia resuelta no se reabre. */
    private SugerenciaRuta buscarPendiente(String uuid) {
        SugerenciaRuta sugerencia = buscar(uuid);
        if (sugerencia.getEstado() != EstadoSugerencia.pendiente) {
            throw new BadRequestException("La sugerencia ya esta " + sugerencia.getEstado());
        }
        return sugerencia;
    }

    /** Recorta y colapsa espacios para comparar y guardar nombres de forma estable. */
    private static String normalizar(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }

    private SugerenciaRutaResponseDTO toResponse(SugerenciaRuta s) {
        return SugerenciaRutaResponseDTO.builder()
                .uuid(s.getUuid())
                .autor(mapper.usuarioSummary(s.getUsuario()))
                .urlFuente(s.getUrlFuente())
                .nombre(s.getNombre())
                .descripcion(s.getDescripcion())
                .puntoInicio(s.getPuntoInicio())
                .puntoFin(s.getPuntoFin())
                .distanciaKm(s.getDistanciaKm())
                .duracionEstimadaMin(s.getDuracionEstimadaMin())
                .dificultad(s.getDificultad())
                .tipoTerreno(s.getTipoTerreno())
                .puntos(leerJson(s.getPuntosJson(), LISTA_PUNTOS))
                .enlacesTrack(leerJson(s.getEnlacesTrackJson(), LISTA_ENLACES))
                .estado(s.getEstado())
                .rutaUuid(s.getRuta() != null ? s.getRuta().getUuid() : null)
                .motivoRechazo(s.getMotivoRechazo())
                .fechaCreacion(s.getFechaCreacion())
                .fechaActualizacion(s.getFechaActualizacion())
                .build();
    }

    private static <T> List<T> leerJson(String json, TypeReference<List<T>> tipo) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, tipo);
        } catch (JacksonException e) {
            // Un JSON corrupto no debe impedir revisar la sugerencia: se muestra sin ese dato.
            return List.of();
        }
    }
}
