import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, ArchiveRestore, ArrowLeft, ExternalLink, Link2, Pencil, Unlink } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Badge, Button, Card, Field, Input, Select } from "@/components/ui";
import { vazioParaNulo } from "@/lib/texto";
import {
  arquivarPlanta,
  ativarPlanta,
  buscarPlanta,
  desvincularSimilar,
  mensagemDeErro,
  opcoesPlantas,
  plantasKeys,
  vincularSimilar,
} from "@/services/plantasApi";
import { rotuloTipoReferencia, type ReferenciaResponse } from "@/types/planta";

function Secao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <Card>
      <h2 className="mb-3 font-bold">{titulo}</h2>
      {children}
    </Card>
  );
}

function TextoOuVazio({ texto }: { texto: string | null | undefined }) {
  if (!texto) return <p className="text-sm text-slate-400">Não informado.</p>;
  return <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-700">{texto}</p>;
}

function Referencia({ r }: { r: ReferenciaResponse }) {
  const titulo = r.titulo ?? null;
  return (
    <li className="py-3 text-sm">
      <div className="mb-1 flex flex-wrap items-center gap-2">
        <Badge tone="info">{rotuloTipoReferencia[r.tipo]}</Badge>
        {r.ano != null && <span className="text-xs text-slate-500">{r.ano}</span>}
      </div>
      {r.autor && <p className="text-slate-600">{r.autor}</p>}
      {titulo &&
        (r.link ? (
          <a
            href={r.link}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 font-semibold text-emerald-900 hover:underline"
          >
            {titulo}
            <ExternalLink size={14} />
          </a>
        ) : (
          <p className="font-semibold">{titulo}</p>
        ))}
      {!titulo && r.link && (
        <a
          href={r.link}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 break-all font-semibold text-emerald-900 hover:underline"
        >
          {r.link}
          <ExternalLink size={14} />
        </a>
      )}
      {r.textoLivre && <p className="mt-1 whitespace-pre-wrap text-slate-700">{r.textoLivre}</p>}
    </li>
  );
}

export function PlantaDetailPage() {
  const { id = "" } = useParams();
  const queryClient = useQueryClient();
  const [aviso, setAviso] = useState<string | null>(null);
  const [similarId, setSimilarId] = useState("");
  const [observacao, setObservacao] = useState("");

  const ficha = useQuery({
    queryKey: plantasKeys.ficha(id),
    queryFn: () => buscarPlanta(id),
    enabled: id !== "",
  });

  const opcoes = useQuery({
    queryKey: plantasKeys.opcoes(),
    queryFn: opcoesPlantas,
    enabled: ficha.data?.ativo === true,
  });

  const invalidar = () => {
    queryClient.invalidateQueries({ queryKey: plantasKeys.all });
  };

  const situacao = useMutation({
    mutationFn: (ativar: boolean) => (ativar ? ativarPlanta(id) : arquivarPlanta(id)),
    onSuccess: (dados) => {
      queryClient.setQueryData(plantasKeys.ficha(id), dados);
      invalidar();
      setAviso(dados.ativo ? "Planta reativada." : "Planta arquivada. Os vínculos de similaridade foram preservados.");
    },
    onError: (e) => setAviso(mensagemDeErro(e)),
  });

  const vincular = useMutation({
    mutationFn: () => vincularSimilar(id, { plantaSimilarId: similarId, observacao: vazioParaNulo(observacao) }),
    onSuccess: (dados) => {
      queryClient.setQueryData(plantasKeys.ficha(id), dados);
      invalidar();
      setSimilarId("");
      setObservacao("");
      setAviso("Vínculo de similaridade criado nos dois sentidos.");
    },
    onError: (e) => setAviso(mensagemDeErro(e)),
  });

  const desvincular = useMutation({
    mutationFn: (outraId: string) => desvincularSimilar(id, outraId),
    onSuccess: () => {
      invalidar();
      setAviso("Vínculo removido dos dois lados.");
    },
    onError: (e) => setAviso(mensagemDeErro(e)),
  });

  if (ficha.isPending) {
    return <p className="text-sm text-slate-500">Carregando ficha…</p>;
  }

  if (ficha.isError) {
    return (
      <Card className="mx-auto max-w-xl border-red-200 text-center">
        <p className="font-semibold text-red-800">Não foi possível carregar a ficha.</p>
        <p className="my-2 text-sm text-slate-600">{mensagemDeErro(ficha.error)}</p>
        <div className="flex justify-center gap-2">
          <Button onClick={() => ficha.refetch()}>Tentar novamente</Button>
          <Link to="/repositorio" className="inline-flex items-center text-sm font-semibold text-emerald-800">
            Voltar ao repositório
          </Link>
        </div>
      </Card>
    );
  }

  const p = ficha.data;
  const jaVinculadas = new Set(p.similares.map((s) => s.id));
  const candidatas = (opcoes.data ?? []).filter((o) => o.id !== p.id && !jaVinculadas.has(o.id));
  const ocupado = situacao.isPending || vincular.isPending || desvincular.isPending;

  return (
    <div className="space-y-5">
      <Link to="/repositorio" className="flex items-center gap-2 text-sm font-semibold text-emerald-800">
        <ArrowLeft size={16} />
        Voltar ao repositório
      </Link>

      {/* 1. Nome científico e família */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold italic text-emerald-950">{p.nomeCientifico}</h1>
          <p className="text-sm text-slate-500">
            {p.familiaBotanica ? `Família ${p.familiaBotanica}` : "Família botânica não informada"}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone={p.ativo ? "good" : "neutral"}>{p.ativo ? "Ativa" : "Arquivada"}</Badge>
          <Link to={`/repositorio/${p.id}/editar`}>
            <Button className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50">
              <Pencil size={16} />
              Editar
            </Button>
          </Link>
          <Button
            className={p.ativo ? "bg-slate-800 hover:bg-slate-900" : ""}
            disabled={ocupado}
            onClick={() => situacao.mutate(!p.ativo)}
          >
            {p.ativo ? <Archive size={16} /> : <ArchiveRestore size={16} />}
            {p.ativo ? "Arquivar" : "Reativar"}
          </Button>
        </div>
      </div>

      {aviso && (
        <div className="rounded-xl bg-amber-50 p-3 text-sm font-medium text-amber-900" role="status">
          {aviso}
        </div>
      )}

      {/* 2. Nomes populares */}
      <Secao titulo="Nomes populares">
        {p.nomesPopulares.length === 0 ? (
          <p className="text-sm text-slate-400">Nenhum nome popular registrado.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {p.nomesPopulares.map((n) => (
              <span
                key={n.id}
                className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm ${
                  n.principal
                    ? "bg-emerald-800 font-semibold text-white"
                    : "bg-stone-100 text-stone-800"
                }`}
              >
                {n.nome}
                {n.principal && <span className="text-[10px] uppercase tracking-wide text-emerald-100">principal</span>}
              </span>
            ))}
          </div>
        )}
      </Secao>

      {/* 3. Cultivo */}
      <Secao titulo="Cultivo">
        <TextoOuVazio texto={p.cultivo} />
      </Secao>

      {/* 4. Indicação de uso */}
      <Secao titulo="Indicação de uso">
        <TextoOuVazio texto={p.indicacaoUso} />
      </Secao>

      {p.observacoes && (
        <Secao titulo="Observações">
          <TextoOuVazio texto={p.observacoes} />
        </Secao>
      )}

      {/* 5. Plantas de uso similares */}
      <Secao titulo="Plantas de uso similares">
        {p.similares.length === 0 ? (
          <p className="text-sm text-slate-400">Nenhuma planta vinculada como similar.</p>
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {p.similares.map((s) => (
              <div
                key={s.id}
                className="flex items-start justify-between gap-3 rounded-xl border border-stone-200 p-4 transition hover:border-emerald-400"
              >
                <Link to={`/repositorio/${s.id}`} className="min-w-0 flex-1">
                  <p className="font-semibold italic text-emerald-900">{s.nomeCientifico}</p>
                  <p className="text-sm text-slate-600">{s.nomePopularPrincipal ?? "Sem nome popular"}</p>
                  {s.observacao && <p className="mt-1 text-xs text-slate-500">{s.observacao}</p>}
                  {!s.ativo && (
                    <div className="mt-2">
                      <Badge tone="neutral">Arquivada</Badge>
                    </div>
                  )}
                </Link>
                <button
                  type="button"
                  className="shrink-0 rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-700 disabled:opacity-50"
                  title="Remover vínculo"
                  aria-label={`Remover vínculo com ${s.nomeCientifico}`}
                  disabled={ocupado}
                  onClick={() => desvincular.mutate(s.id)}
                >
                  <Unlink size={16} />
                </button>
              </div>
            ))}
          </div>
        )}

        {p.ativo && (
          <div className="mt-4 grid gap-3 rounded-xl bg-stone-50 p-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
            <Field label="Vincular planta similar">
              <Select value={similarId} onChange={(e) => setSimilarId(e.target.value)} disabled={opcoes.isPending}>
                <option value="">{opcoes.isPending ? "Carregando…" : "Selecione uma planta"}</option>
                {candidatas.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.nomeCientifico}
                    {o.nomePopularPrincipal ? ` · ${o.nomePopularPrincipal}` : ""}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Observação (por que são similares)">
              <Input
                value={observacao}
                maxLength={500}
                onChange={(e) => setObservacao(e.target.value)}
                placeholder="Ex.: ambas usadas como expectorante"
              />
            </Field>
            <Button disabled={!similarId || ocupado} onClick={() => vincular.mutate()}>
              <Link2 size={16} />
              Vincular
            </Button>
          </div>
        )}
      </Secao>

      {/* 6. Referências */}
      <Secao titulo="Referências">
        {p.referencias.length === 0 ? (
          <p className="text-sm text-slate-400">Nenhuma referência registrada.</p>
        ) : (
          <ol className="list-decimal divide-y divide-stone-100 pl-5 marker:font-semibold marker:text-slate-400">
            {p.referencias.map((r) => (
              <Referencia key={r.id} r={r} />
            ))}
          </ol>
        )}
      </Secao>

      <Secao titulo="Itens de estoque">
        <p className="text-sm text-slate-500">
          O vínculo com os itens de estoque será exibido aqui quando o módulo de Itens for implementado.
        </p>
      </Secao>
    </div>
  );
}
