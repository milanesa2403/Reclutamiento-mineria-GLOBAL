package com.mineria.reclutamiento.modules.documento.storage;

public class AlmacenamientoNoConfiguradoException extends RuntimeException {

    public AlmacenamientoNoConfiguradoException() {
        super("Almacenamiento de archivos no configurado");
    }
}
