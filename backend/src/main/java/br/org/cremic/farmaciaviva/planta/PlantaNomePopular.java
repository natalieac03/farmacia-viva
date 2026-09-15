package br.org.cremic.farmaciaviva.planta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Nome popular de uma planta.
 *
 * Unico por (planta, nome normalizado). Plantas diferentes PODEM compartilhar
 * o mesmo nome popular: e exatamente essa ambiguidade que o repositorio existe
 * para tornar visivel.
 */
@Entity
@Table(name = "planta_nome_popular")
@Getter
@Setter
@NoArgsConstructor
public class PlantaNomePopular {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planta_id", nullable = false)
    private Planta planta;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 150)
    private String nomeNormalizado;

    @Column(name = "principal", nullable = false)
    private boolean principal;

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
        PlantaNomePopular outroNome = (PlantaNomePopular) outro;
        return this.id != null && Objects.equals(this.id, outroNome.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
