package dev.deveps.moteros.mapper;

import dev.deveps.moteros.dto.*;
import dev.deveps.moteros.entities.*;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversion entidad -&gt; DTO. Es un colaborador "tonto": no accede a repositorios.
 * Los valores calculados (contadores, medias, flags relativos al usuario que consulta)
 * los calcula el servicio y se pasan como parametros.
 */
@Component
public class EntityDtoMapper {

    // ===================== USUARIO =====================

    public UsuarioSummaryDTO usuarioSummary(Usuario u) {
        if (u == null) return null;
        return UsuarioSummaryDTO.builder()
                .uuid(u.getUuid())
                .nombreUsuario(u.getNombreUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .fotoPerfilUrl(u.getFotoPerfilUrl())
                .ciudad(u.getCiudad())
                .build();
    }

    public UsuarioResponseDTO usuarioResponse(Usuario u, Long numMotos, Long numRutas, Long numAmigos) {
        if (u == null) return null;
        return UsuarioResponseDTO.builder()
                .uuid(u.getUuid())
                .nombreUsuario(u.getNombreUsuario())
                .nombreCompleto(u.getNombreCompleto())
                .email(u.getEmail())
                .ciudad(u.getCiudad())
                .biografia(u.getBiografia())
                .fotoPerfilUrl(u.getFotoPerfilUrl())
                .fechaRegistro(u.getFechaRegistro())
                .activo(u.getActivo())
                .numMotos(numMotos)
                .numRutas(numRutas)
                .numAmigos(numAmigos)
                .build();
    }

    // ===================== MOTO =====================

    public MotoSummaryDTO motoSummary(Moto m) {
        if (m == null) return null;
        return MotoSummaryDTO.builder()
                .uuid(m.getUuid())
                .marca(m.getMarca())
                .modelo(m.getModelo())
                .anio(m.getAnio())
                .tipo(m.getTipo())
                .fotoUrl(m.getFotoUrl())
                .build();
    }

    public MotoResponseDTO motoResponse(Moto m) {
        if (m == null) return null;
        return MotoResponseDTO.builder()
                .uuid(m.getUuid())
                .propietario(usuarioSummary(m.getUsuario()))
                .marca(m.getMarca())
                .modelo(m.getModelo())
                .anio(m.getAnio())
                .cilindradaCc(m.getCilindradaCc())
                .tipo(m.getTipo())
                .fotoUrl(m.getFotoUrl())
                .build();
    }

    // ===================== PUNTO / VALORACION DE RUTA =====================

    public PuntoRutaResponseDTO puntoRutaResponse(PuntoRuta p) {
        if (p == null) return null;
        return PuntoRutaResponseDTO.builder()
                .uuid(p.getUuid())
                .orden(p.getOrden())
                .latitud(p.getLatitud())
                .longitud(p.getLongitud())
                .altitudM(p.getAltitudM())
                .nombrePunto(p.getNombrePunto())
                .build();
    }

    public ValoracionRutaResponseDTO valoracionResponse(ValoracionRuta v) {
        if (v == null) return null;
        return ValoracionRutaResponseDTO.builder()
                .uuid(v.getUuid())
                .rutaUuid(v.getRuta() != null ? v.getRuta().getUuid() : null)
                .autor(usuarioSummary(v.getUsuario()))
                .puntuacion(v.getPuntuacion() != null ? v.getPuntuacion().intValue() : null)
                .comentario(v.getComentario())
                .fecha(v.getFecha())
                .build();
    }

    // ===================== RUTA =====================

    public RutaSummaryDTO rutaSummary(Ruta r, Double valoracionMedia, Long numValoraciones) {
        if (r == null) return null;
        return RutaSummaryDTO.builder()
                .uuid(r.getUuid())
                .nombre(r.getNombre())
                .creador(usuarioSummary(r.getCreador()))
                .puntoInicio(r.getPuntoInicio())
                .puntoFin(r.getPuntoFin())
                .distanciaKm(r.getDistanciaKm())
                .duracionEstimadaMin(r.getDuracionEstimadaMin())
                .dificultad(r.getDificultad())
                .tipoTerreno(r.getTipoTerreno())
                .valoracionMedia(valoracionMedia)
                .numValoraciones(numValoraciones)
                .build();
    }

    public RutaResponseDTO rutaResponse(Ruta r, Double valoracionMedia, Long numValoraciones,
                                        List<PuntoRutaResponseDTO> puntos) {
        if (r == null) return null;
        return RutaResponseDTO.builder()
                .uuid(r.getUuid())
                .creador(usuarioSummary(r.getCreador()))
                .nombre(r.getNombre())
                .descripcion(r.getDescripcion())
                .puntoInicio(r.getPuntoInicio())
                .latitudInicio(r.getLatitudInicio())
                .longitudInicio(r.getLongitudInicio())
                .puntoFin(r.getPuntoFin())
                .latitudFin(r.getLatitudFin())
                .longitudFin(r.getLongitudFin())
                .distanciaKm(r.getDistanciaKm())
                .duracionEstimadaMin(r.getDuracionEstimadaMin())
                .dificultad(r.getDificultad())
                .tipoTerreno(r.getTipoTerreno())
                .fechaCreacion(r.getFechaCreacion())
                .valoracionMedia(valoracionMedia)
                .numValoraciones(numValoraciones)
                .puntos(puntos)
                .build();
    }

    // ===================== QUEDADA =====================

    public QuedadaSummaryDTO quedadaSummary(Quedada q, Long numInscritos) {
        if (q == null) return null;
        return QuedadaSummaryDTO.builder()
                .uuid(q.getUuid())
                .titulo(q.getTitulo())
                .organizador(usuarioSummary(q.getOrganizador()))
                .rutaUuid(q.getRuta() != null ? q.getRuta().getUuid() : null)
                .rutaNombre(q.getRuta() != null ? q.getRuta().getNombre() : null)
                .puntoEncuentro(q.getPuntoEncuentro())
                .fechaHora(q.getFechaHora())
                .maxParticipantes(q.getMaxParticipantes())
                .numInscritos(numInscritos)
                .nivelRecomendado(q.getNivelRecomendado())
                .estado(q.getEstado())
                .build();
    }

    public QuedadaResponseDTO quedadaResponse(Quedada q, Long numInscritos, Integer plazasLibres,
                                              EstadoInscripcion inscripcionUsuarioActual,
                                              List<InscripcionQuedadaResponseDTO> inscritos,
                                              Double rutaMedia, Long rutaNumValoraciones) {
        if (q == null) return null;
        return QuedadaResponseDTO.builder()
                .uuid(q.getUuid())
                .organizador(usuarioSummary(q.getOrganizador()))
                .ruta(rutaSummary(q.getRuta(), rutaMedia, rutaNumValoraciones))
                .titulo(q.getTitulo())
                .descripcion(q.getDescripcion())
                .puntoEncuentro(q.getPuntoEncuentro())
                .latitudEncuentro(q.getLatitudEncuentro())
                .longitudEncuentro(q.getLongitudEncuentro())
                .fechaHora(q.getFechaHora())
                .maxParticipantes(q.getMaxParticipantes())
                .nivelRecomendado(q.getNivelRecomendado())
                .estado(q.getEstado())
                .fechaCreacion(q.getFechaCreacion())
                .numInscritos(numInscritos)
                .plazasLibres(plazasLibres)
                .inscripcionUsuarioActual(inscripcionUsuarioActual)
                .inscritos(inscritos)
                .build();
    }

    public InscripcionQuedadaResponseDTO inscripcionResponse(InscripcionQuedada i) {
        if (i == null) return null;
        return InscripcionQuedadaResponseDTO.builder()
                .uuid(i.getUuid())
                .quedadaUuid(i.getQuedada() != null ? i.getQuedada().getUuid() : null)
                .usuario(usuarioSummary(i.getUsuario()))
                .estado(i.getEstado())
                .fechaInscripcion(i.getFechaInscripcion())
                .build();
    }

    // ===================== PUBLICACION / COMENTARIO / LIKE =====================

    public ComentarioResponseDTO comentarioResponse(Comentario c) {
        if (c == null) return null;
        return ComentarioResponseDTO.builder()
                .uuid(c.getUuid())
                .publicacionUuid(c.getPublicacion() != null ? c.getPublicacion().getUuid() : null)
                .autor(usuarioSummary(c.getUsuario()))
                .contenido(c.getContenido())
                .fecha(c.getFecha())
                .build();
    }

    public LikePublicacionResponseDTO likeResponse(LikePublicacion l) {
        if (l == null) return null;
        return LikePublicacionResponseDTO.builder()
                .uuid(l.getUuid())
                .publicacionUuid(l.getPublicacion() != null ? l.getPublicacion().getUuid() : null)
                .usuario(usuarioSummary(l.getUsuario()))
                .fecha(l.getFecha())
                .build();
    }

    public PublicacionResponseDTO publicacionResponse(Publicacion p, Long numLikes, Long numComentarios,
                                                      Boolean likeUsuarioActual,
                                                      List<ComentarioResponseDTO> comentarios,
                                                      Double rutaMedia, Long rutaNumValoraciones) {
        if (p == null) return null;
        return PublicacionResponseDTO.builder()
                .uuid(p.getUuid())
                .autor(usuarioSummary(p.getUsuario()))
                .ruta(rutaSummary(p.getRuta(), rutaMedia, rutaNumValoraciones))
                .contenido(p.getContenido())
                .imagenUrl(p.getImagenUrl())
                .fechaPublicacion(p.getFechaPublicacion())
                .numLikes(numLikes)
                .numComentarios(numComentarios)
                .likeUsuarioActual(likeUsuarioActual)
                .comentarios(comentarios)
                .build();
    }

    // ===================== AMISTAD =====================

    public AmistadResponseDTO amistadResponse(Amistad a, Integer idUsuarioActual) {
        if (a == null) return null;
        boolean enviadaPorMi = a.getUsuario() != null
                && a.getUsuario().getId().equals(idUsuarioActual);
        return AmistadResponseDTO.builder()
                .uuid(a.getUuid())
                .solicitante(usuarioSummary(a.getUsuario()))
                .destinatario(usuarioSummary(a.getAmigo()))
                .estado(a.getEstado())
                .fecha(a.getFecha())
                .enviadaPorMi(enviadaPorMi)
                .build();
    }

    // ===================== CHAT =====================

    public MensajeResponseDTO mensajeResponse(Mensaje m, Integer idUsuarioActual) {
        if (m == null) return null;
        boolean propio = m.getRemitente() != null
                && m.getRemitente().getId().equals(idUsuarioActual);
        return MensajeResponseDTO.builder()
                .uuid(m.getUuid())
                .conversacionUuid(m.getConversacion() != null ? m.getConversacion().getUuid() : null)
                .remitente(usuarioSummary(m.getRemitente()))
                .contenido(m.getContenido())
                .leido(m.getLeido())
                .fechaEnvio(m.getFechaEnvio())
                .propio(propio)
                .build();
    }

    public ConversacionResponseDTO conversacionResponse(Conversacion c, UsuarioSummaryDTO interlocutor,
                                                        MensajeResponseDTO ultimoMensaje, Long numNoLeidos) {
        if (c == null) return null;
        return ConversacionResponseDTO.builder()
                .uuid(c.getUuid())
                .interlocutor(interlocutor)
                .ultimoMensaje(ultimoMensaje)
                .numNoLeidos(numNoLeidos)
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }

    // ===================== NOTIFICACION =====================

    public NotificacionResponseDTO notificacionResponse(Notificacion n) {
        if (n == null) return null;
        return NotificacionResponseDTO.builder()
                .uuid(n.getUuid())
                .tipo(n.getTipo())
                .mensaje(n.getMensaje())
                .referenciaId(n.getReferenciaId())
                .usuarioOrigen(usuarioSummary(n.getUsuarioOrigen()))
                .leido(n.getLeido())
                .fechaCreacion(n.getFechaCreacion())
                .build();
    }
}
