package br.org.cremic.farmaciaviva.planta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Ficha completa de uma planta. */
public record PlantaResponse(
    UUID id,
    String nomeCientifico,
    String familiaBotanica,
    String cultivo,
    String indicacaoUso,
    String observacoes,
    Boolean ativo,
    Long versao,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm,
    List<NomePopularResponse> nomesPopulares,
    List<ReferenciaResponse> referencias,
    List<SimilarResponse> similares
) {

    public record NomePopularResponse(
        UUID id,
        String nome,
        Boolean principal,
        Integer ordem
    ) {
    }

    public record ReferenciaResponse(
        UUID id,
        TipoReferencia tipo,
        String autor,
        String titulo,
        Integer ano,
        String link,
        String textoLivre,
        Integer ordem
    ) {
    }

    /**
     * A outra ponta de um vinculo de similaridade. {@code id} e o id da OUTRA
     * planta, para que o frontend navegue direto para a ficha dela.
     */
    public record SimilarResponse(
        UUID id,
        String nomeCientifico,
        String nomePopularPrincipal,
        Boolean ativo,
        String observacao
    ) {
    }
}
