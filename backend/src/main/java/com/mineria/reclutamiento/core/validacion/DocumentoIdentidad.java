package com.mineria.reclutamiento.core.validacion;

import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DocumentoIdentidad {

    public static final String PERU = "PE";

    private static final Pattern PAIS = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern GENERICO = Pattern.compile("^[A-Z0-9-]{5,20}$");
    private static final Pattern DNI_PERU = Pattern.compile("^\\d{8}$");
    private static final Pattern RUC_EMPRESA_PERU = Pattern.compile("^20\\d{9}$");

    private DocumentoIdentidad() {
    }

    public static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }

    public static String problemaPais(String pais) {
        return PAIS.matcher(normalizar(pais)).matches() ? null : "Selecciona un país válido.";
    }

    public static String problemaDocumento(String pais, TipoDocumentoIdentidad tipo, String numero) {
        String limpio = normalizar(numero);
        if (tipo == null) {
            return "Selecciona el tipo de documento.";
        }
        if (PERU.equals(normalizar(pais)) && tipo == TipoDocumentoIdentidad.DNI) {
            return DNI_PERU.matcher(limpio).matches() ? null : "El DNI peruano debe tener 8 dígitos.";
        }
        return GENERICO.matcher(limpio).matches()
                ? null
                : "El número de documento debe tener entre 5 y 20 letras o números.";
    }

    public static String problemaIdentificadorFiscal(String pais, String numero) {
        String limpio = normalizar(numero);
        if (PERU.equals(normalizar(pais))) {
            return RUC_EMPRESA_PERU.matcher(limpio).matches()
                    ? null
                    : "El RUC de la empresa debe empezar con 20 y tener 11 dígitos.";
        }
        return GENERICO.matcher(limpio).matches()
                ? null
                : "El identificador fiscal debe tener entre 5 y 20 letras o números.";
    }
}
