package com.mineria.reclutamiento.modules.postulante.entity;

import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum DocumentoPostulante {

    CV("cv", "el CV", TipoDocumento.CV, Set.of(DocumentoService.PDF),
            Postulante::getUrlCv, Postulante::setUrlCv),
    FOTO_DNI("foto-dni", "el DNI", TipoDocumento.FOTO_DNI,
            Set.of(DocumentoService.PDF, DocumentoService.JPEG, DocumentoService.PNG),
            Postulante::getUrlFotoDni, Postulante::setUrlFotoDni),
    CUL("cul", "el CUL", TipoDocumento.CUL, Set.of(DocumentoService.PDF),
            Postulante::getUrlCul, Postulante::setUrlCul),
    EMO("emo", "el EMO", TipoDocumento.EMO,
            Set.of(DocumentoService.PDF, DocumentoService.JPEG, DocumentoService.PNG),
            Postulante::getUrlEmo, Postulante::setUrlEmo);

    private final String ruta;
    private final String nombre;
    private final TipoDocumento tipoAlmacenamiento;
    private final Set<String> formatos;
    private final Function<Postulante, String> lector;
    private final BiConsumer<Postulante, String> escritor;

    DocumentoPostulante(String ruta, String nombre, TipoDocumento tipoAlmacenamiento, Set<String> formatos,
                        Function<Postulante, String> lector, BiConsumer<Postulante, String> escritor) {
        this.ruta = ruta;
        this.nombre = nombre;
        this.tipoAlmacenamiento = tipoAlmacenamiento;
        this.formatos = formatos;
        this.lector = lector;
        this.escritor = escritor;
    }

    public String ruta() { return ruta; }

    public String nombre() { return nombre; }

    public TipoDocumento tipoAlmacenamiento() { return tipoAlmacenamiento; }

    public Set<String> formatos() { return formatos; }

    public String leer(Postulante p) { return lector.apply(p); }

    public void escribir(Postulante p, String clave) { escritor.accept(p, clave); }

    public static Optional<DocumentoPostulante> desdeRuta(String ruta) {
        return Arrays.stream(values()).filter(d -> d.ruta.equals(ruta)).findFirst();
    }
}
