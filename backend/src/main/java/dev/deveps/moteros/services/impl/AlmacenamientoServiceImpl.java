package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.ArchivoSubidoDTO;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.services.AlmacenamientoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class AlmacenamientoServiceImpl implements AlmacenamientoService {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path directorio;
    private final String publicPath;

    public AlmacenamientoServiceImpl(@Value("${app.uploads.dir:uploads}") String dir,
                                     @Value("${app.uploads.public-path:/uploads}") String publicPath) {
        this.directorio = Paths.get(dir).toAbsolutePath().normalize();
        this.publicPath = publicPath.endsWith("/") ? publicPath.substring(0, publicPath.length() - 1) : publicPath;
    }

    @Override
    public ArchivoSubidoDTO guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("No se ha recibido ningun archivo");
        }
        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : EXTENSIONES.get(tipo.toLowerCase());
        if (extension == null) {
            throw new BadRequestException("Formato no soportado. Se admiten JPG, PNG, WEBP y GIF");
        }

        String nombre = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombre).normalize();
            if (!destino.startsWith(directorio)) {
                throw new BadRequestException("Nombre de archivo invalido");
            }
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new BadRequestException("No se pudo guardar el archivo: " + e.getMessage());
        }

        return ArchivoSubidoDTO.builder()
                .url(publicPath + "/" + nombre)
                .nombreArchivo(nombre)
                .tipoContenido(tipo)
                .tamano(archivo.getSize())
                .build();
    }
}
