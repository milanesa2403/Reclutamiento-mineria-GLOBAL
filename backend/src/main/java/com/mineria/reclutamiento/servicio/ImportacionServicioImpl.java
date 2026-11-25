package com.mineria.reclutamiento.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.repositorio.PerfilRepositorio;
import com.mineria.reclutamiento.repositorio.UsuarioRepositorio;
import com.mineria.reclutamiento.util.EstandaresIso;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportacionServicioImpl implements ImportacionServicio {
    private record Fila(int numero, Map<String, String> datos) {}

    private final OfertaServicio ofertas;
    private final UsuarioRepositorio usuarios;
    private final PerfilRepositorio perfiles;
    private final AuditoriaServicio auditoria;
    private final ObjectMapper mapper;

    public ImportacionServicioImpl(OfertaServicio ofertas, UsuarioRepositorio usuarios, PerfilRepositorio perfiles,
            AuditoriaServicio auditoria, ObjectMapper mapper) {
        this.ofertas = ofertas;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.auditoria = auditoria;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Reporte importarOfertas(UUID usuarioEmpresaId, byte[] archivo, String formato) {
        ofertas.empresaDe(usuarioEmpresaId);
        return procesar(archivo, formato, f -> {
            Map<String, String> d = f.datos();
            ofertas.publicar(usuarioEmpresaId, new OfertaServicio.DatosOferta(
                    d.get("titulo"), d.get("descripcion"), d.get("especialidadRequerida"),
                    d.get("pais"), numero(d.get("salario"), "salario"), d.get("moneda")));
        });
    }

    @Override
    @Transactional
    public Reporte importarPerfiles(UUID adminId, byte[] archivo, String formato) {
        auditoria.registrar(adminId, "IMPORTACION_PERFILES", "perfiles", true);
        return procesar(archivo, formato, f -> {
            Map<String, String> d = f.datos();
            Usuario u = usuarios.findByCorreo(vacio(d.get("correo")).toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Correo no registrado"));
            if (u.getRol() != Rol.TRABAJADOR) throw new IllegalArgumentException("El usuario no es trabajador");
            Perfil p = perfiles.findById(u.getId()).orElseThrow(() -> new IllegalArgumentException("Perfil inexistente"));
            if (!vacio(d.get("pais")).isBlank()) p.setPais(EstandaresIso.pais(d.get("pais")));
            if (!vacio(d.get("idioma")).isBlank()) p.setIdioma(EstandaresIso.idioma(d.get("idioma")));
            if (!vacio(d.get("especialidad")).isBlank()) p.setEspecialidad(d.get("especialidad"));
            if (!vacio(d.get("aniosExperiencia")).isBlank()) {
                int a = (int) numero(d.get("aniosExperiencia"), "aniosExperiencia");
                if (a < 0 || a > 70) throw new IllegalArgumentException("aniosExperiencia fuera de rango");
                p.setAniosExperiencia(a);
            }
        });
    }

    @Override
    public byte[] exportarOfertas(UUID usuarioEmpresaId, String formato) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Oferta o : ofertas.listarDeEmpresa(usuarioEmpresaId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("titulo", o.getTitulo());
            m.put("descripcion", o.getDescripcion());
            m.put("especialidadRequerida", o.getEspecialidadRequerida());
            m.put("pais", o.getPais());
            m.put("salario", o.getSalario());
            m.put("moneda", o.getMoneda());
            m.put("estado", o.getEstado());
            m.put("fechaPublicacion", o.getFechaPublicacion()); // ISO 8601 UTC
            filas.add(m);
        }
        return exportar(filas, formato);
    }

    @Override
    public byte[] exportarPerfiles(UUID adminId, String formato) {
        auditoria.registrar(adminId, "EXPORTACION_PERFILES", "perfiles", true);
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Perfil p : perfiles.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("usuarioId", p.getUsuarioId());
            m.put("nombreCompleto", p.getNombreCompleto());
            m.put("pais", p.getPais());
            m.put("idioma", p.getIdioma());
            m.put("especialidad", p.getEspecialidad());
            m.put("aniosExperiencia", p.getAniosExperiencia());
            filas.add(m);
        }
        return exportar(filas, formato);
    }

    // ---------- helpers ----------
    private Reporte procesar(byte[] archivo, String formato, Consumer<Fila> accion) {
        if (archivo == null || archivo.length == 0) throw new IllegalArgumentException("El archivo esta vacio");
        if (archivo.length > MAX_BYTES) throw new IllegalArgumentException("El archivo supera los 5 MB");
        List<Fila> filas = leer(archivo, formato);
        List<ErrorFila> errores = new ArrayList<>();
        int ok = 0;
        for (Fila f : filas) {
            try {
                if (f.datos().containsKey("__columnas")) throw new IllegalArgumentException("Numero de columnas distinto al encabezado");
                accion.accept(f);
                ok++;
            } catch (IllegalArgumentException | IllegalStateException e) {
                errores.add(new ErrorFila(f.numero(), e.getMessage()));
            }
        }
        return new Reporte(filas.size(), ok, errores);
    }

    private List<Fila> leer(byte[] archivo, String formato) {
        String texto = new String(archivo, StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
        String f = formato == null ? "" : formato.toLowerCase();
        if (f.equals("json")) return leerJson(texto);
        if (f.equals("csv")) return leerCsv(texto);
        throw new IllegalArgumentException("Formato no soportado: use csv o json");
    }

    private List<Fila> leerJson(String texto) {
        try {
            JsonNode raiz = mapper.readTree(texto);
            if (raiz == null || !raiz.isArray()) throw new IllegalArgumentException("El JSON debe ser un arreglo de objetos");
            List<Fila> filas = new ArrayList<>();
            int i = 1;
            for (JsonNode n : raiz) {
                Map<String, String> m = new HashMap<>();
                if (n.isObject()) n.fields().forEachRemaining(e -> m.put(e.getKey(), e.getValue().isNull() ? "" : e.getValue().asText()));
                filas.add(new Fila(i++, m));
            }
            return filas;
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON invalido");
        }
    }

    private static List<Fila> leerCsv(String texto) {
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder c = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (comillas) {
                if (ch == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') { c.append('"'); i++; } else comillas = false;
                } else c.append(ch);
            } else if (ch == '"') comillas = true;
            else if (ch == ',') { fila.add(c.toString()); c.setLength(0); }
            else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                fila.add(c.toString());
                c.setLength(0);
                if (!(fila.size() == 1 && fila.get(0).isBlank())) filas.add(fila);
                fila = new ArrayList<>();
            } else c.append(ch);
        }
        if (c.length() > 0 || !fila.isEmpty()) { fila.add(c.toString()); filas.add(fila); }
        if (filas.isEmpty()) throw new IllegalArgumentException("El CSV no tiene encabezado");
        List<String> cab = filas.get(0).stream().map(String::trim).toList();
        List<Fila> res = new ArrayList<>();
        for (int i = 1; i < filas.size(); i++) {
            List<String> v = filas.get(i);
            Map<String, String> m = new HashMap<>();
            if (v.size() == cab.size()) for (int j = 0; j < cab.size(); j++) m.put(cab.get(j), v.get(j).trim());
            else m.put("__columnas", "mal");
            res.add(new Fila(i + 1, m)); // numero de fila contando el encabezado como fila 1
        }
        return res;
    }

    private byte[] exportar(List<Map<String, Object>> filas, String formato) {
        String f = formato == null ? "" : formato.toLowerCase();
        try {
            if (f.equals("json")) return mapper.writeValueAsBytes(filas);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!f.equals("csv")) throw new IllegalArgumentException("Formato no soportado: use csv o json");
        StringBuilder sb = new StringBuilder();
        if (!filas.isEmpty()) {
            sb.append(String.join(",", filas.get(0).keySet())).append("\n");
            for (Map<String, Object> m : filas) {
                sb.append(String.join(",", m.values().stream().map(ImportacionServicioImpl::celda).toList())).append("\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String celda(Object o) {
        String s = o == null ? "" : o.toString();
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String vacio(String s) { return s == null ? "" : s.trim(); }

    private static double numero(String s, String campo) {
        try {
            return Double.parseDouble(vacio(s));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " no es numerico");
        }
    }
}
