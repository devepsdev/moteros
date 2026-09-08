package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.MotoRequestDTO;
import dev.deveps.moteros.dto.MotoResponseDTO;
import dev.deveps.moteros.entities.Moto;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoMoto;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.MotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MotoServiceImpl implements MotoService {

    private final MotoRepository motoRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<MotoResponseDTO> listarMisMotos() {
        String uuid = usuarioAutenticado.obtenerUuidUsuarioActual();
        return motoRepository.findByUsuarioUuidOrderByIdDesc(uuid).stream()
                .map(mapper::motoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotoResponseDTO> listarPorUsuario(String usuarioUuid) {
        return motoRepository.findByUsuarioUuidOrderByIdDesc(usuarioUuid).stream()
                .map(mapper::motoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MotoResponseDTO obtenerPorUuid(String uuid) {
        return mapper.motoResponse(buscar(uuid));
    }

    @Override
    public MotoResponseDTO crear(MotoRequestDTO dto) {
        Usuario propietario = usuarioAutenticado.obtenerUsuarioActual();
        Moto moto = Moto.builder()
                .usuario(propietario)
                .marca(dto.getMarca())
                .modelo(dto.getModelo())
                .anio(dto.getAnio())
                .cilindradaCc(dto.getCilindradaCc())
                .tipo(dto.getTipo() != null ? dto.getTipo() : TipoMoto.naked)
                .fotoUrl(dto.getFotoUrl())
                .build();
        return mapper.motoResponse(motoRepository.save(moto));
    }

    @Override
    public MotoResponseDTO actualizar(String uuid, MotoRequestDTO dto) {
        Moto moto = buscar(uuid);
        exigirPropietario(moto);

        moto.setMarca(dto.getMarca());
        moto.setModelo(dto.getModelo());
        moto.setAnio(dto.getAnio());
        moto.setCilindradaCc(dto.getCilindradaCc());
        if (dto.getTipo() != null) {
            moto.setTipo(dto.getTipo());
        }
        moto.setFotoUrl(dto.getFotoUrl());

        return mapper.motoResponse(motoRepository.save(moto));
    }

    @Override
    public void eliminar(String uuid) {
        Moto moto = buscar(uuid);
        exigirPropietario(moto);
        motoRepository.delete(moto);
    }

    private Moto buscar(String uuid) {
        return motoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Moto no encontrada: " + uuid));
    }

    private void exigirPropietario(Moto moto) {
        Integer actual = usuarioAutenticado.obtenerIdUsuarioActual();
        if (!moto.getUsuario().getId().equals(actual)) {
            throw new BadRequestException("No puedes modificar una moto que no es tuya");
        }
    }
}
