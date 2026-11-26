package com.mineria.reclutamiento.modules.documento.storage;

public interface AlmacenamientoArchivos {

    void subir(String clave, byte[] contenido, String tipoMime);

    void eliminar(String clave);

    String urlFirmada(String clave, int segundos);

    String bucket();
}
