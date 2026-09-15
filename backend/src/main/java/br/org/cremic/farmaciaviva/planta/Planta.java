package br.org.cremic.farmaciaviva.planta;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Ficha de uma planta do repositorio.
 *
 * O nome cientifico e a identidade da planta: a forma normalizada e unica no
 * banco. Nomes populares e referencias sao filhos da ficha (cascade e
 * orphanRemoval). Os vinculos de similaridade NAO sao mapeados aqui de
 * proposito: sao gerenciados por {@link PlantaSimilar} e seu repositorio, para
 * que arquivar ou editar uma planta nunca toque nos vinculos por efeito
 * colateral de cascade.
 */
@Entity
@Table(name = "planta")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Planta {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "nome_cientifico", nullable = false, length = 200)
    private String nomeCientifico;

    @Column(name = "nome_cientifico_normalizado", nullable = false, length = 200)
    private String nomeCientificoNormalizado;

    @Column(name = "familia_botanica", length = 120)
    private String familiaBotanica;

    @Column(name = "cultivo", columnDefinition = "text")
    private String cultivo;

    @Column(name = "indicacao_uso", columnDefinition = "text")
    private String indicacaoUso;

    @Column(name = "observacoes", columnDefinition = "text")
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @OneToMany(mappedBy = "planta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC, nome ASC")
    private List<PlantaNomePopular> nomesPopulares = new ArrayList<>();

    @OneToMany(mappedBy = "planta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PlantaReferencia> referencias = new ArrayList<>();

    public void adicionarNomePopular(PlantaNomePopular nomePopular) {
        nomePopular.setPlanta(this);
        nomesPopulares.add(nomePopular);
    }

    public void adicionarReferencia(PlantaReferencia referencia) {
        referencia.setPlanta(this);
        referencias.add(referencia);
    }

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
        Planta outraPlanta = (Planta) outro;
        return this.id != null && Objects.equals(this.id, outraPlanta.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
