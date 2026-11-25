package com.mineria.reclutamiento.servicio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlmacenLocal implements AlmacenArchivos {
    private final Path base;

    public AlmacenLocal(@Value("${app.almacen.ruta:./almacen}") String ruta) {
        this.base = Path.of(ruta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path ruta(String sha) {
        if (!sha.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Hash invalido");
        return base.resolve(sha);
    }

    @Override public boolean existe(String sha) { return Files.exists(ruta(sha)); }

    @Override
    public void guardar(String sha, byte[] contenido) {
        try { Files.write(ruta(sha), contenido); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    @Override
    public byte[] leer(String sha) {
        try { return Files.readAllBytes(ruta(sha)); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    @Override
    public void eliminar(String sha) {
        try { Files.deleteIfExists(ruta(sha)); } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
