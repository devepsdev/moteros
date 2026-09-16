package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.entities.enums.MotivoDenuncia;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Denuncia para la bandeja de moderacion del panel. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenunciaResponseDTO {

    private String uuid;
    private UsuarioSummaryDTO denunciante;
    private UsuarioSummaryDTO denunciado;
    /** Si el denunciado sigue activo; null si ha borrado su cuenta. */
    private Boolean denunciadoActivo;
    /** Total de denuncias recibidas por el denunciado, incluida esta. */
    private long denunciasContraDenunciado;
    private TipoDenuncia tipo;
    private String referenciaUuid;
    /** Si el contenido denunciado sigue existiendo. */
    private boolean contenidoExiste;
    private MotivoDenuncia motivo;
    private String descripcion;
    private String contenido;
    private String imagenUrl;
    private EstadoDenuncia estado;
    private boolean contenidoEliminado;
    private boolean usuarioDadoDeBaja;
    private String notaResolucion;
    private UsuarioSummaryDTO resueltaPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
