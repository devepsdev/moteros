package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.EstadisticasGlobalesDTO;
import dev.deveps.moteros.dto.EstadisticasGlobalesDTO.AltasMesDTO;
import dev.deveps.moteros.dto.RutaSummaryDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.ComentarioRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.repositories.ValoracionRutaRepository;
import dev.deveps.moteros.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UsuarioRepository usuarioRepository;
    private final MotoRepository motoRepository;
    private final RutaRepository rutaRepository;
    private final QuedadaRepository quedadaRepository;
    private final PublicacionRepository publicacionRepository;
    private final ComentarioRepository comentarioRepository;
    private final ValoracionRutaRepository valoracionRutaRepository;
    private final AmistadRepository amistadRepository;
    private final EntityDtoMapper mapper;

    @Override
    public EstadisticasGlobalesDTO estadisticasGlobales() {
        return EstadisticasGlobalesDTO.builder()
                .usuariosTotales(usuarioRepository.count())
                .usuariosActivos(usuarioRepository.countByActivoTrue())
                .administradores(usuarioRepository.countByRol(RolUsuario.admin))
                .motos(motoRepository.count())
                .rutas(rutaRepository.count())
                .quedadas(quedadaRepository.count())
                .quedadasProximas(quedadaRepository.countByFechaHoraAfter(LocalDateTime.now()))
                .publicaciones(publicacionRepository.count())
                .comentarios(comentarioRepository.count())
                .valoraciones(valoracionRutaRepository.count())
                .amistadesAceptadas(amistadRepository.countByEstado(EstadoAmistad.aceptada))
                .rutasPorDificultad(aMapa(rutaRepository.contarPorDificultad()))
                .rutasPorTerreno(aMapa(rutaRepository.contarPorTerreno()))
                .motosPorTipo(aMapa(motoRepository.contarPorTipo()))
                .quedadasPorEstado(aMapa(quedadaRepository.contarPorEstado()))
                .topRutasPorValoracion(topRutasPorValoracion())
                .altasUsuariosPorMes(altasUsuariosPorMes())
                .build();
    }

    @Override
    public Page<UsuarioResponseDTO> listarUsuarios(String texto, Pageable pageable) {
        return usuarioRepository.buscarTodosPorTexto(texto, pageable)
                .map(u -> mapper.usuarioResponse(u, null, null, null));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarRol(String usuarioUuid, RolUsuario rol) {
        Usuario usuario = usuarioRepository.findByUuid(usuarioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioUuid));

        boolean degradaAdmin = usuario.getRol() == RolUsuario.admin && rol == RolUsuario.user;
        if (degradaAdmin && usuarioRepository.countByRol(RolUsuario.admin) <= 1) {
            throw new BadRequestException("No puedes dejar la plataforma sin administradores");
        }

        usuario.setRol(rol);
        Usuario guardado = usuarioRepository.save(usuario);
        return mapper.usuarioResponse(guardado, null, null, null);
    }

    // ===================== PRIVADOS =====================

    private List<RutaSummaryDTO> topRutasPorValoracion() {
        List<Object[]> medias = valoracionRutaRepository.mediaPorRutaDesc(PageRequest.of(0, 5));
        List<Integer> ids = medias.stream().map(f -> (Integer) f[0]).toList();
        Map<Integer, Ruta> porId = rutaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ruta::getId, r -> r));

        return medias.stream()
                .map(f -> {
                    Ruta ruta = porId.get((Integer) f[0]);
                    if (ruta == null) {
                        return null;
                    }
                    Double media = f[1] == null
                            ? null
                            : Math.round(((Number) f[1]).doubleValue() * 100.0) / 100.0;
                    long n = valoracionRutaRepository.countByRutaId(ruta.getId());
                    return mapper.rutaSummary(ruta, media, n);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<AltasMesDTO> altasUsuariosPorMes() {
        Map<String, Long> porMes = usuarioRepository.fechasRegistro().stream()
                .collect(Collectors.groupingBy(f -> YearMonth.from(f).toString(), Collectors.counting()));

        return porMes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByKey().reversed())
                .limit(12)
                .map(e -> AltasMesDTO.builder().mes(e.getKey()).total(e.getValue()).build())
                .toList();
    }

    private Map<String, Long> aMapa(List<Object[]> filas) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            if (fila[0] == null) {
                continue;
            }
            mapa.put(fila[0].toString(), ((Number) fila[1]).longValue());
        }
        return mapa;
    }
}
