package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.DenunciaRequestDTO;
import dev.deveps.moteros.dto.DenunciaResponseDTO;
import dev.deveps.moteros.dto.ResolverDenunciaDTO;
import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DenunciaService {

    /** Registra la denuncia del usuario actual. Repetirla mientras esta pendiente no duplica nada. */
    void denunciar(DenunciaRequestDTO dto);

    Page<DenunciaResponseDTO> listar(EstadoDenuncia estado, Pageable pageable);

    DenunciaResponseDTO obtener(String uuid);

    /** Aplica las medidas y cierra esta denuncia y las demas pendientes sobre el mismo contenido. */
    DenunciaResponseDTO resolver(String uuid, ResolverDenunciaDTO dto);
}
