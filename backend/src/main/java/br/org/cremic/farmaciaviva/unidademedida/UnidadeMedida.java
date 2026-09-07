package br.org.cremic.farmaciaviva.unidademedida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "unidade_medida")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class UnidadeMedida {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimensao", nullable = false, length = 20)
    private Dimensao dimensao;

    @Column(name = "fator_para_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal fatorParaBase;

    @Column(name = "unidade_base", nullable = false)
    private boolean unidadeBase;

    @Column(name = "casas_decimais", nullable = false)
    private int casasDecimais;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null) {
            return false;
        }
        Class<?> classeDesteObjeto = this instanceof HibernateProxy proxy
            ? proxy.getHibernateLazyInitializer().getPersistentClass()
            : getClass();
        Class<?> classeDoOutroObjeto = outro instanceof HibernateProxy proxy
            ? proxy.getHibernateLazyInitializer().getPersistentClass()
            : outro.getClass();
        if (!classeDesteObjeto.equals(classeDoOutroObjeto)) {
            return false;
        }
        UnidadeMedida outraUnidade = (UnidadeMedida) outro;
        return this.id != null && Objects.equals(this.id, outraUnidade.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
