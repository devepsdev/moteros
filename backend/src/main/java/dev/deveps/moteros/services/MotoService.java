package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.MotoRequestDTO;
import dev.deveps.moteros.dto.MotoResponseDTO;

import java.util.List;

public interface MotoService {

    List<MotoResponseDTO> listarMisMotos();

    List<MotoResponseDTO> listarPorUsuario(String usuarioUuid);

    MotoResponseDTO obtenerPorUuid(String uuid);

    MotoResponseDTO crear(MotoRequestDTO dto);

    MotoResponseDTO actualizar(String uuid, MotoRequestDTO dto);

    void eliminar(String uuid);
}
