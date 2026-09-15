import { z } from "zod";
import { normalizarTexto } from "@/lib/texto";

/*
 * Espelho campo a campo dos DTOs de `br.org.cremic.farmaciaviva.planta`:
 *   PlantaResponse (+ NomePopularResponse, ReferenciaResponse, SimilarResponse)
 *   PlantaResumoResponse, PlantaOpcaoResponse, PageResponse
 *   PlantaRequest (+ NomePopularRequest, ReferenciaRequest), VincularSimilarRequest
 *
 * ATENÇÃO à nullability: o backend serializa com
 * `spring.jackson.default-property-inclusion: non_null`, então um campo nulo
 * NÃO vem no JSON. Por isso todo campo nullable de resposta é `.nullish()`
 * (aceita ausente ou null) e nunca apenas `.nullable()` — com `.nullable()` o
 * parse falharia em produção assim que um campo opcional viesse vazio.
 */

export const tiposReferencia = ["LIVRO", "ARTIGO", "LEGISLACAO", "SITE", "OUTRO"] as const;
export const tipoReferenciaSchema = z.enum(tiposReferencia);
export type TipoReferencia = z.infer<typeof tipoReferenciaSchema>;

export const rotuloTipoReferencia: Record<TipoReferencia, string> = {
  LIVRO: "Livro",
  ARTIGO: "Artigo",
  LEGISLACAO: "Legislação",
  SITE: "Site",
  OUTRO: "Outro",
};

// ---------------------------------------------------------------
// Respostas
// ---------------------------------------------------------------

export const nomePopularResponseSchema = z.object({
  id: z.string().uuid(),
  nome: z.string(),
  principal: z.boolean(),
  ordem: z.number().int(),
});

export const referenciaResponseSchema = z.object({
  id: z.string().uuid(),
  tipo: tipoReferenciaSchema,
  autor: z.string().nullish(),
  titulo: z.string().nullish(),
  ano: z.number().int().nullish(),
  link: z.string().nullish(),
  textoLivre: z.string().nullish(),
  ordem: z.number().int(),
});

export const similarResponseSchema = z.object({
  id: z.string().uuid(),
  nomeCientifico: z.string(),
  nomePopularPrincipal: z.string().nullish(),
  ativo: z.boolean(),
  observacao: z.string().nullish(),
});

export const plantaResponseSchema = z.object({
  id: z.string().uuid(),
  nomeCientifico: z.string(),
  familiaBotanica: z.string().nullish(),
  cultivo: z.string().nullish(),
  indicacaoUso: z.string().nullish(),
  observacoes: z.string().nullish(),
  ativo: z.boolean(),
  versao: z.number().int(),
  criadoEm: z.string(),
  atualizadoEm: z.string(),
  nomesPopulares: z.array(nomePopularResponseSchema),
  referencias: z.array(referenciaResponseSchema),
  similares: z.array(similarResponseSchema),
});

export const plantaResumoSchema = z.object({
  id: z.string().uuid(),
  nomeCientifico: z.string(),
  familiaBotanica: z.string().nullish(),
  nomesPopulares: z.array(z.string()),
  quantidadeSimilares: z.number().int(),
  ativo: z.boolean(),
});

export const plantaOpcaoSchema = z.object({
  id: z.string().uuid(),
  nomeCientifico: z.string(),
  nomePopularPrincipal: z.string().nullish(),
});

export function pageResponseSchema<T extends z.ZodTypeAny>(item: T) {
  return z.object({
    content: z.array(item),
    page: z.number().int(),
    size: z.number().int(),
    totalElements: z.number().int(),
    totalPages: z.number().int(),
    first: z.boolean(),
    last: z.boolean(),
  });
}

export type PlantaResponse = z.infer<typeof plantaResponseSchema>;
export type PlantaResumo = z.infer<typeof plantaResumoSchema>;
export type PlantaOpcao = z.infer<typeof plantaOpcaoSchema>;
export type ReferenciaResponse = z.infer<typeof referenciaResponseSchema>;
export type SimilarResponse = z.infer<typeof similarResponseSchema>;
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

// ---------------------------------------------------------------
// Requisições (o que vai pelo fio; mesmos limites do Bean Validation)
// ---------------------------------------------------------------

export const nomePopularRequestSchema = z.object({
  nome: z.string().trim().min(1).max(150),
  principal: z.boolean(),
});

export const referenciaRequestSchema = z
  .object({
    tipo: tipoReferenciaSchema,
    autor: z.string().trim().max(250).nullable(),
    titulo: z.string().trim().max(500).nullable(),
    ano: z.number().int().positive().nullable(),
    link: z.string().trim().max(1000).nullable(),
    textoLivre: z.string().trim().nullable(),
  })
  .refine((r) => Boolean(r.titulo) || Boolean(r.textoLivre), {
    message: "Informe o título ou o texto livre",
    path: ["titulo"],
  });

export const plantaRequestSchema = z.object({
  nomeCientifico: z.string().trim().min(1).max(200),
  familiaBotanica: z.string().trim().max(120).nullable(),
  cultivo: z.string().trim().nullable(),
  indicacaoUso: z.string().trim().nullable(),
  observacoes: z.string().trim().nullable(),
  nomesPopulares: z.array(nomePopularRequestSchema),
  referencias: z.array(referenciaRequestSchema),
});

export const vincularSimilarRequestSchema = z.object({
  plantaSimilarId: z.string().uuid(),
  observacao: z.string().trim().max(500).nullable(),
});

export type PlantaRequest = z.infer<typeof plantaRequestSchema>;
export type VincularSimilarRequest = z.infer<typeof vincularSimilarRequestSchema>;

// ---------------------------------------------------------------
// Formulário (campos como texto; convertido para PlantaRequest no envio)
// ---------------------------------------------------------------

const anoFormSchema = z
  .string()
  .trim()
  .refine((v) => v === "" || /^\d{1,4}$/.test(v), { message: "Ano inválido" });

export const referenciaFormSchema = z
  .object({
    tipo: tipoReferenciaSchema,
    autor: z.string().max(250, "No máximo 250 caracteres"),
    titulo: z.string().max(500, "No máximo 500 caracteres"),
    ano: anoFormSchema,
    link: z.string().max(1000, "No máximo 1000 caracteres"),
    textoLivre: z.string(),
  })
  .refine((r) => r.titulo.trim() !== "" || r.textoLivre.trim() !== "", {
    message: "Informe o título ou o texto livre",
    path: ["titulo"],
  });

export const plantaFormSchema = z
  .object({
    nomeCientifico: z
      .string()
      .trim()
      .min(1, "Informe o nome científico")
      .max(200, "No máximo 200 caracteres"),
    familiaBotanica: z.string().max(120, "No máximo 120 caracteres"),
    cultivo: z.string(),
    indicacaoUso: z.string(),
    observacoes: z.string(),
    nomesPopulares: z.array(
      z.object({
        nome: z.string().trim().min(1, "Informe o nome popular").max(150, "No máximo 150 caracteres"),
        principal: z.boolean(),
      }),
    ),
    referencias: z.array(referenciaFormSchema),
  })
  .superRefine((form, ctx) => {
    const vistos = new Map<string, number>();
    let principais = 0;
    form.nomesPopulares.forEach((n, i) => {
      const chave = normalizarTexto(n.nome);
      const anterior = vistos.get(chave);
      if (anterior !== undefined) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Repete o nome popular da linha ${anterior + 1}`,
          path: ["nomesPopulares", i, "nome"],
        });
      } else {
        vistos.set(chave, i);
      }
      if (n.principal) principais += 1;
    });
    if (principais > 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Apenas um nome popular pode ser o principal",
        path: ["nomesPopulares"],
      });
    }
  });

export type PlantaForm = z.infer<typeof plantaFormSchema>;
