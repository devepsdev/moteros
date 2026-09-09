package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.ArchivoSubidoDTO;
import dev.deveps.moteros.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoServiceImplTest {

    @TempDir
    Path tempDir;

    private AlmacenamientoServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new AlmacenamientoServiceImpl(tempDir.toString(), "/uploads");
    }

    @Test
    void guardarImagen_png_guardaArchivoYDevuelveUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", new byte[]{1, 2, 3, 4});

        ArchivoSubidoDTO res = servicio.guardarImagen(file);

        assertThat(res.getUrl()).startsWith("/uploads/").endsWith(".png");
        assertThat(res.getTamano()).isEqualTo(4);
        assertThat(Files.exists(tempDir.resolve(res.getNombreArchivo()))).isTrue();
    }

    @Test
    void guardarImagen_formatoNoImagen_lanzaBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> servicio.guardarImagen(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void guardarImagen_archivoVacio_lanzaBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vacio.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> servicio.guardarImagen(file))
                .isInstanceOf(BadRequestException.class);
    }
}
