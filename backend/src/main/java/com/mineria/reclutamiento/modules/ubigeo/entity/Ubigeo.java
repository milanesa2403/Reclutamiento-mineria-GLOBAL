package com.mineria.reclutamiento.modules.ubigeo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "ubigeo")
public class Ubigeo {

    @Id
    @Column(name = "codigo", length = 6, columnDefinition = "bpchar(6)")
    private String codigo;

    @Column(name = "departamento", nullable = false, length = 60)
    private String departamento;

    @Column(name = "provincia", nullable = false, length = 60)
    private String provincia;

    @Column(name = "distrito", nullable = false, length = 80)
    private String distrito;

    @Column(name = "cod_departamento", insertable = false, updatable = false, columnDefinition = "bpchar(2)")
    private String codDepartamento;

    @Column(name = "cod_provincia", insertable = false, updatable = false, columnDefinition = "bpchar(4)")
    private String codProvincia;

    protected Ubigeo() {
    }

    public String getCodigo() { return codigo; }
    public String getDepartamento() { return departamento; }
    public String getProvincia() { return provincia; }
    public String getDistrito() { return distrito; }
    public String getCodDepartamento() { return codDepartamento; }
    public String getCodProvincia() { return codProvincia; }
}
