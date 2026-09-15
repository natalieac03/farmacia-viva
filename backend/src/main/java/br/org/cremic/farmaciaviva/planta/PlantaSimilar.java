package br.org.cremic.farmaciaviva.planta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Vinculo de uso similar entre duas plantas (autorrelacionamento).
 *
 * DECISAO: o vinculo e armazenado DIRECIONADO, uma linha por sentido.
 * Ao vincular A a B, o service grava duas linhas: (A -> B) e (B -> A), com a
 * mesma observacao. Ao desvincular, remove as duas.
 *
 * Por que assim, e nao uma unica linha simetrica:
 *   - a leitura fica simetrica ("similar a" vale nos dois sentidos, que e
 *     como a pessoa pensa) com uma consulta simples por planta_id, sem OR
 *     nas duas colunas e sem ordenacao canonica de UUID para decidir qual
 *     lado da relacao guarda a linha;
 *   - o custo e uma linha extra por vinculo, irrelevante para o volume
 *     deste cadastro.
 *
 * A invariante "toda linha (A -> B) tem sua reciproca (B -> A)" e garantida
 * pelo service, dentro de uma unica transacao. Nao ha cascade a partir de
 * {@link Planta}: arquivar uma planta preserva os vinculos, e ela continua
 * aparecendo como similar das outras, marcada como arquivada.
 */
@Entity
@Table(name = "planta_similar")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PlantaSimilar {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planta_id", nullable = false)
    private Planta planta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planta_similar_id", nullable = false)
    private Planta plantaSimilar;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

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
        PlantaSimilar outroVinculo = (PlantaSimilar) outro;
        return this.id != null && Objects.equals(this.id, outroVinculo.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
