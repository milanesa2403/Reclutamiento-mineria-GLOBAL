package com.mineria.reclutamiento.servicio;

/** Abstraccion del almacenamiento. Local en desarrollo; S3 cifrado (AES-256) en AWS. La clave es el SHA-256. */
public interface AlmacenArchivos {
    boolean existe(String sha256);
    void guardar(String sha256, byte[] contenido);
    byte[] leer(String sha256);
    void eliminar(String sha256);
}
