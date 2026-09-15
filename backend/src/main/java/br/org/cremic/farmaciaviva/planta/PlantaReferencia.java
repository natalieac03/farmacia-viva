package br.org.cremic.farmaciaviva.planta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;

/**
 * Referencia bibliografica de uma planta.
 *
 * Referencia botanica vem de origens muito diferentes (livro, RDC da Anvisa,
 * artigo, anotacao de campo). Por isso os campos estruturados sao todos
 * opcionais e o texto livre e a saida para o que nao couber no formato. A
 * unica exigencia, no banco e no service, e titulo OU texto livre.
 */
@Entity
@Table(name = "planta_referencia")
@Getter
@Setter
@NoArgsConstructor
public class PlantaReferencia {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planta_id", nullable = false)
    private Planta planta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoReferencia tipo;

    @Column(name = "autor", length = 250)
    private String autor;

    @Column(name = "titulo", length = 500)
    private String titulo;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "link", length = 1000)
    private String link;

    @Column(name = "texto_livre", columnDefinition = "text")
    private String textoLivre;

    @Column(name = "ordem", nullable = false)
    private int ordem;

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
        PlantaReferencia outraReferencia = (PlantaReferencia) outro;
        return this.id != null && Objects.equals(this.id, outraReferencia.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
