import { ZodError } from "zod";
import { ApiError, apiDelete, apiGet, apiPatch, apiPost, apiPut } from "@/lib/api";
import {
  pageResponseSchema,
  plantaOpcaoSchema,
  plantaRequestSchema,
  plantaResponseSchema,
  plantaResumoSchema,
  vincularSimilarRequestSchema,
  type PageResponse,
  type PlantaOpcao,
  type PlantaRequest,
  type PlantaResponse,
  type PlantaResumo,
  type VincularSimilarRequest,
} from "@/types/planta";

const BASE = "/api/v1/plantas";

export type FiltroPlantas = {
  busca?: string;
  familiaBotanica?: string;
  ativo?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export const plantasKeys = {
  all: ["plantas"] as const,
  lista: (filtro: FiltroPlantas) => [...plantasKeys.all, "lista", filtro] as const,
  ficha: (id: string) => [...plantasKeys.all, "ficha", id] as const,
  opcoes: () => [...plantasKeys.all, "opcoes"] as const,
};

const paginaDeResumos = pageResponseSchema(plantaResumoSchema);

export async function listarPlantas(filtro: FiltroPlantas): Promise<PageResponse<PlantaResumo>> {
  const qs = new URLSearchParams();
  if (filtro.busca) qs.set("busca", filtro.busca);
  if (filtro.familiaBotanica) qs.set("familiaBotanica", filtro.familiaBotanica);
  if (filtro.ativo !== undefined) qs.set("ativo", String(filtro.ativo));
  qs.set("page", String(filtro.page ?? 0));
  qs.set("size", String(filtro.size ?? 20));
  qs.set("sort", filtro.sort ?? "nomeCientifico,asc");
  return paginaDeResumos.parse(await apiGet<unknown>(`${BASE}?${qs.toString()}`));
}

export async function buscarPlanta(id: string): Promise<PlantaResponse> {
  return plantaResponseSchema.parse(await apiGet<unknown>(`${BASE}/${id}`));
}

export async function criarPlanta(request: PlantaRequest): Promise<PlantaResponse> {
  // parse antes de enviar: garante que o que sai daqui é exatamente o PlantaRequest do backend
  return plantaResponseSchema.parse(await apiPost<unknown>(BASE, plantaRequestSchema.parse(request)));
}

export async function atualizarPlanta(id: string, request: PlantaRequest): Promise<PlantaResponse> {
  return plantaResponseSchema.parse(
    await apiPut<unknown>(`${BASE}/${id}`, plantaRequestSchema.parse(request)),
  );
}

export async function arquivarPlanta(id: string): Promise<PlantaResponse> {
  return plantaResponseSchema.parse(await apiPatch<unknown>(`${BASE}/${id}/arquivar`));
}

export async function ativarPlanta(id: string): Promise<PlantaResponse> {
  return plantaResponseSchema.parse(await apiPatch<unknown>(`${BASE}/${id}/ativar`));
}

export async function vincularSimilar(
  id: string,
  request: VincularSimilarRequest,
): Promise<PlantaResponse> {
  return plantaResponseSchema.parse(
    await apiPost<unknown>(`${BASE}/${id}/similares`, vincularSimilarRequestSchema.parse(request)),
  );
}

export async function desvincularSimilar(id: string, similarId: string): Promise<void> {
  await apiDelete(`${BASE}/${id}/similares/${similarId}`);
}

export async function opcoesPlantas(): Promise<PlantaOpcao[]> {
  return plantaOpcaoSchema.array().parse(await apiGet<unknown>(`${BASE}/opcoes`));
}

/**
 * Converte qualquer erro em uma mensagem em português para a tela.
 *
 * As mensagens do backend vêm sem acento (padrão do projeto); as mais comuns
 * são traduzidas aqui. O 409 de nome científico duplicado informa QUAL planta
 * já usa o nome — o backend manda "...: <nome> (id <uuid>)" e extraímos o nome.
 */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ZodError) {
    return "A resposta do servidor não está no formato esperado. Verifique a versão do backend.";
  }
  if (!(erro instanceof ApiError)) {
    return "Erro inesperado. Tente novamente.";
  }

  const m = erro.message;

  if (erro.status === 0) return m;

  if (erro.status === 409) {
    const nome = /nome cientifico: (.+?) \(id [0-9a-f-]+\)/i.exec(m)?.[1];
    if (nome) return `Já existe uma planta cadastrada com este nome científico: ${nome}.`;
    if (/vinculadas como similares/i.test(m)) return "Essas plantas já estão vinculadas como similares.";
    return "Conflito com um registro existente. Recarregue a página e tente novamente.";
  }

  if (erro.status === 404) {
    if (/vinculo/i.test(m)) return "Esse vínculo de similaridade não existe mais.";
    return "Planta não encontrada.";
  }

  if (erro.status === 422) {
    if (/dela mesma/i.test(m)) return "Uma planta não pode ser vinculada como similar dela mesma.";
    if (/arquivada/i.test(m)) return "Plantas arquivadas não podem ser vinculadas como similares.";
    if (/titulo ou texto livre/i.test(m)) return "Cada referência precisa de título ou texto livre.";
    if (/repetido/i.test(m)) return "Há nomes populares repetidos nesta planta.";
    if (/principal/i.test(m)) return "Apenas um nome popular pode ser o principal.";
    return m;
  }

  if (erro.status === 400) {
    if (erro.fieldErrors.length > 0) {
      return `Dados inválidos: ${erro.fieldErrors.map((f) => `${f.field} — ${f.message}`).join("; ")}.`;
    }
    return "Dados inválidos. Revise o formulário.";
  }

  if (erro.status >= 500) return "O servidor encontrou um erro inesperado. Tente novamente em instantes.";

  return m;
}
