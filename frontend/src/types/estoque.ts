import { z } from "zod";

export const tecnicas = ["Homeopatia", "Fitoterapia", "Essências florais", "Óleos essenciais", "Uso geral"] as const;
export const tiposLote = ["Planta seca", "Planta processada", "Insumo", "Produto acabado"] as const;
export const origens = ["Produção interna CREMIC", "Compra de terceiro", "Doação", "Transferência", "Outra"] as const;
export const qualidades = ["Pendente", "Em quarentena", "Aprovado", "Reprovado", "Bloqueado", "Não se aplica"] as const;
export const situacoesLote = ["Ativo", "Esgotado", "Finalizado", "Arquivado"] as const;
export const tiposMovimento = ["Entrada", "Saída", "Transferência", "Ajuste positivo", "Ajuste negativo", "Estorno"] as const;

export const itemSchema = z.object({ id:z.string(), codigo:z.string(), nome:z.string(), tecnica:z.enum(tecnicas), apresentacao:z.string(), unidade:z.string(), estoqueMinimo:z.number().optional(), ativo:z.boolean() });
export const localSchema = z.object({ id:z.string(), nome:z.string(), descricao:z.string().optional(), ativo:z.boolean() });
export const loteSchema = z.object({
  id:z.string(), codigo:z.string(), itemId:z.string(), tipo:z.enum(tiposLote), origem:z.enum(origens), origemTexto:z.string(), fabricante:z.string().optional(), fabricacao:z.string().optional(), recebimento:z.string(), validade:z.string(), qualidade:z.enum(qualidades), exigeCQ:z.boolean(), lcq:z.string().optional(), laudoAgronomico:z.string().optional(), laudoMicrobiologico:z.string().optional(), ordemProducao:z.string().optional(), observacoes:z.string(), situacao:z.enum(situacoesLote), criadoEm:z.string(), atualizadoEm:z.string(), finalizadoEm:z.string().optional(), motivoFinalizacao:z.string().optional(), responsavel:z.string()
});
export const movimentoSchema = z.object({ id:z.string(), tipo:z.enum(tiposMovimento), itemId:z.string(), loteId:z.string(), quantidade:z.number().positive(), unidade:z.string(), origemId:z.string().optional(), destinoId:z.string().optional(), motivo:z.string(), observacao:z.string(), responsavel:z.string(), dataHora:z.string(), estornaMovimentoId:z.string().optional(), situacao:z.literal("Confirmada") });
export const databaseSchema = z.object({ schemaVersion:z.literal(1), items:z.array(itemSchema), locais:z.array(localSchema), lotes:z.array(loteSchema), movimentos:z.array(movimentoSchema) });

export type Item = z.infer<typeof itemSchema>;
export type Localizacao = z.infer<typeof localSchema>;
export type Lote = z.infer<typeof loteSchema>;
export type Movimento = z.infer<typeof movimentoSchema>;
export type Database = z.infer<typeof databaseSchema>;
export type TipoMovimento = typeof tiposMovimento[number];

export type Saldo = { itemId:string; loteId:string; localId:string; quantidade:number };
export type NovaMovimentacao = Omit<Movimento, "id"|"dataHora"|"situacao">;
