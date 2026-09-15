package br.org.cremic.farmaciaviva.planta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Corpo de criacao e de atualizacao da ficha de uma planta.
 *
 * O mesmo DTO serve ao POST e ao PUT: a ficha e substituida por inteiro,
 * incluindo as listas de nomes populares e referencias (a ordem das listas e
 * a ordem exibida). Similares nao entram aqui: vinculam-se pela ficha, porque
 * dependem de a outra planta ja existir.
 */
public record PlantaRequest(

    @NotBlank(message = "nomeCientifico e obrigatorio")
    @Size(max = 200, message = "nomeCientifico deve ter no maximo 200 caracteres")
    String nomeCientifico,

    @Size(max = 120, message = "familiaBotanica deve ter no maximo 120 caracteres")
    String familiaBotanica,

    String cultivo,

    String indicacaoUso,

    String observacoes,

    @Valid
    List<NomePopularRequest> nomesPopulares,

    @Valid
    List<ReferenciaRequest> referencias
) {

    public record NomePopularRequest(

        @NotBlank(message = "nome do nome popular e obrigatorio")
        @Size(max = 150, message = "nome popular deve ter no maximo 150 caracteres")
        String nome,

        Boolean principal
    ) {
    }

    public record ReferenciaRequest(

        @NotNull(message = "tipo da referencia e obrigatorio")
        TipoReferencia tipo,

        @Size(max = 250, message = "autor deve ter no maximo 250 caracteres")
        String autor,

        @Size(max = 500, message = "titulo deve ter no maximo 500 caracteres")
        String titulo,

        @Positive(message = "ano deve ser positivo")
        Integer ano,

        @Size(max = 1000, message = "link deve ter no maximo 1000 caracteres")
        String link,

        String textoLivre
    ) {
    }
}
