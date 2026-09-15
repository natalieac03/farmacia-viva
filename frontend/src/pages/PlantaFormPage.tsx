import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button, Card, Field, Input, Select, Textarea } from "@/components/ui";
import { vazioParaNulo } from "@/lib/texto";
import { atualizarPlanta, buscarPlanta, criarPlanta, mensagemDeErro, plantasKeys } from "@/services/plantasApi";
import {
  plantaFormSchema,
  rotuloTipoReferencia,
  tiposReferencia,
  type PlantaForm,
  type PlantaRequest,
  type PlantaResponse,
} from "@/types/planta";

const FORM_VAZIO: PlantaForm = {
  nomeCientifico: "",
  familiaBotanica: "",
  cultivo: "",
  indicacaoUso: "",
  observacoes: "",
  nomesPopulares: [{ nome: "", principal: true }],
  referencias: [],
};

const REFERENCIA_VAZIA: PlantaForm["referencias"][number] = {
  tipo: "LIVRO",
  autor: "",
  titulo: "",
  ano: "",
  link: "",
  textoLivre: "",
};

/** Form (strings) -> PlantaRequest (o DTO do backend, com null onde vazio). */
function paraRequest(f: PlantaForm): PlantaRequest {
  return {
    nomeCientifico: f.nomeCientifico.trim(),
    familiaBotanica: vazioParaNulo(f.familiaBotanica),
    cultivo: vazioParaNulo(f.cultivo),
    indicacaoUso: vazioParaNulo(f.indicacaoUso),
    observacoes: vazioParaNulo(f.observacoes),
    nomesPopulares: f.nomesPopulares.map((n) => ({ nome: n.nome.trim(), principal: n.principal })),
    referencias: f.referencias.map((r) => ({
      tipo: r.tipo,
      autor: vazioParaNulo(r.autor),
      titulo: vazioParaNulo(r.titulo),
      ano: r.ano.trim() === "" ? null : Number(r.ano),
      link: vazioParaNulo(r.link),
      textoLivre: vazioParaNulo(r.textoLivre),
    })),
  };
}

function paraForm(p: PlantaResponse): PlantaForm {
  return {
    nomeCientifico: p.nomeCientifico,
    familiaBotanica: p.familiaBotanica ?? "",
    cultivo: p.cultivo ?? "",
    indicacaoUso: p.indicacaoUso ?? "",
    observacoes: p.observacoes ?? "",
    nomesPopulares: p.nomesPopulares.map((n) => ({ nome: n.nome, principal: n.principal })),
    referencias: p.referencias.map((r) => ({
      tipo: r.tipo,
      autor: r.autor ?? "",
      titulo: r.titulo ?? "",
      ano: r.ano != null ? String(r.ano) : "",
      link: r.link ?? "",
      textoLivre: r.textoLivre ?? "",
    })),
  };
}

function Erro({ mensagem }: { mensagem?: string }) {
  if (!mensagem) return null;
  return <p className="text-xs font-medium text-red-700">{mensagem}</p>;
}

export function PlantaFormPage() {
  const { id } = useParams();
  const editando = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState<string | null>(null);

  const ficha = useQuery({
    queryKey: plantasKeys.ficha(id ?? ""),
    queryFn: () => buscarPlanta(id ?? ""),
    enabled: editando,
  });

  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<PlantaForm>({
    resolver: zodResolver(plantaFormSchema),
    defaultValues: FORM_VAZIO,
  });

  useEffect(() => {
    if (ficha.data) reset(paraForm(ficha.data));
  }, [ficha.data, reset]);

  const nomes = useFieldArray({ control, name: "nomesPopulares" });
  const referencias = useFieldArray({ control, name: "referencias" });

  const salvar = useMutation({
    mutationFn: (form: PlantaForm) =>
      editando ? atualizarPlanta(id ?? "", paraRequest(form)) : criarPlanta(paraRequest(form)),
    onSuccess: (dados) => {
      queryClient.setQueryData(plantasKeys.ficha(dados.id), dados);
      queryClient.invalidateQueries({ queryKey: plantasKeys.all });
      navigate(`/repositorio/${dados.id}`);
    },
    onError: (e) => setErroApi(mensagemDeErro(e)),
  });

  const marcarPrincipal = (indice: number, marcado: boolean) => {
    if (!marcado) return;
    nomes.fields.forEach((_, j) => {
      if (j !== indice) setValue(`nomesPopulares.${j}.principal`, false);
    });
  };

  if (editando && ficha.isPending) {
    return <p className="text-sm text-slate-500">Carregando ficha…</p>;
  }

  if (editando && ficha.isError) {
    return (
      <Card className="mx-auto max-w-xl border-red-200 text-center">
        <p className="font-semibold text-red-800">Não foi possível carregar a planta para edição.</p>
        <p className="my-2 text-sm text-slate-600">{mensagemDeErro(ficha.error)}</p>
        <Button onClick={() => ficha.refetch()}>Tentar novamente</Button>
      </Card>
    );
  }

  const voltarPara = editando ? `/repositorio/${id}` : "/repositorio";
  const erroNomes = errors.nomesPopulares?.root?.message ?? errors.nomesPopulares?.message;

  return (
    <form
      className="mx-auto max-w-4xl space-y-5"
      onSubmit={handleSubmit((form) => {
        setErroApi(null);
        salvar.mutate(form);
      })}
      noValidate
    >
      <Link to={voltarPara} className="flex items-center gap-2 text-sm font-semibold text-emerald-800">
        <ArrowLeft size={16} />
        {editando ? "Voltar à ficha" : "Voltar ao repositório"}
      </Link>

      <div>
        <h1 className="text-2xl font-bold">{editando ? "Editar planta" : "Nova planta"}</h1>
        <p className="text-sm text-slate-500">
          O nome científico identifica a planta; duas fichas para a mesma espécie são recusadas.
        </p>
      </div>

      {erroApi && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-900" role="alert">
          {erroApi}
        </div>
      )}

      <Card className="grid gap-4 md:grid-cols-2">
        <div className="md:col-span-2">
          <Field label="Nome científico">
            <Input {...register("nomeCientifico")} placeholder="Ex.: Mikania glomerata Spreng." maxLength={200} />
          </Field>
          <Erro mensagem={errors.nomeCientifico?.message} />
        </div>
        <div>
          <Field label="Família botânica">
            <Input {...register("familiaBotanica")} placeholder="Ex.: Asteraceae" maxLength={120} />
          </Field>
          <Erro mensagem={errors.familiaBotanica?.message} />
        </div>
      </Card>

      <Card className="grid gap-4">
        <Field label="Cultivo">
          <Textarea {...register("cultivo")} rows={3} placeholder="Propagação, solo, luz, colheita…" />
        </Field>
        <Field label="Indicação de uso">
          <Textarea {...register("indicacaoUso")} rows={3} placeholder="Uso tradicional e reconhecido…" />
        </Field>
        <Field label="Observações">
          <Textarea {...register("observacoes")} rows={2} />
        </Field>
      </Card>

      <Card>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-bold">Nomes populares</h2>
          <Button
            type="button"
            className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50"
            onClick={() => nomes.append({ nome: "", principal: nomes.fields.length === 0 })}
          >
            <Plus size={16} />
            Adicionar
          </Button>
        </div>
        <Erro mensagem={erroNomes} />
        {nomes.fields.length === 0 && (
          <p className="text-sm text-slate-400">Nenhum nome popular. Opcional, mas é o principal caminho de busca.</p>
        )}
        <div className="space-y-2">
          {nomes.fields.map((campo, i) => {
            const principal = register(`nomesPopulares.${i}.principal`);
            return (
              <div key={campo.id} className="grid gap-2 md:grid-cols-[1fr_auto_auto] md:items-start">
                <div>
                  <Input {...register(`nomesPopulares.${i}.nome`)} placeholder="Ex.: Guaco" maxLength={150} aria-label={`Nome popular ${i + 1}`} />
                  <Erro mensagem={errors.nomesPopulares?.[i]?.nome?.message} />
                </div>
                <label className="flex items-center gap-2 py-2.5 text-sm">
                  <input
                    type="checkbox"
                    {...principal}
                    onChange={(e) => {
                      void principal.onChange(e);
                      marcarPrincipal(i, e.target.checked);
                    }}
                  />
                  Principal
                </label>
                <button
                  type="button"
                  className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-700"
                  aria-label={`Remover nome popular ${i + 1}`}
                  onClick={() => nomes.remove(i)}
                >
                  <Trash2 size={16} />
                </button>
              </div>
            );
          })}
        </div>
      </Card>

      <Card>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-bold">Referências</h2>
          <Button
            type="button"
            className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50"
            onClick={() => referencias.append({ ...REFERENCIA_VAZIA })}
          >
            <Plus size={16} />
            Adicionar
          </Button>
        </div>
        {referencias.fields.length === 0 && (
          <p className="text-sm text-slate-400">Nenhuma referência. Cada referência precisa de título ou texto livre.</p>
        )}
        <div className="space-y-4">
          {referencias.fields.map((campo, i) => {
            const e = errors.referencias?.[i];
            return (
              <div key={campo.id} className="rounded-xl border border-stone-200 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Referência {i + 1}</span>
                  <button
                    type="button"
                    className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-700"
                    aria-label={`Remover referência ${i + 1}`}
                    onClick={() => referencias.remove(i)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
                <div className="grid gap-3 md:grid-cols-[160px_1fr_100px]">
                  <Field label="Tipo">
                    <Select {...register(`referencias.${i}.tipo`)}>
                      {tiposReferencia.map((t) => (
                        <option key={t} value={t}>
                          {rotuloTipoReferencia[t]}
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <div>
                    <Field label="Autor">
                      <Input {...register(`referencias.${i}.autor`)} maxLength={250} />
                    </Field>
                    <Erro mensagem={e?.autor?.message} />
                  </div>
                  <div>
                    <Field label="Ano">
                      <Input {...register(`referencias.${i}.ano`)} inputMode="numeric" maxLength={4} />
                    </Field>
                    <Erro mensagem={e?.ano?.message} />
                  </div>
                  <div className="md:col-span-3">
                    <Field label="Título">
                      <Input {...register(`referencias.${i}.titulo`)} maxLength={500} />
                    </Field>
                    <Erro mensagem={e?.titulo?.message} />
                  </div>
                  <div className="md:col-span-3">
                    <Field label="Link">
                      <Input {...register(`referencias.${i}.link`)} type="url" maxLength={1000} placeholder="https://" />
                    </Field>
                    <Erro mensagem={e?.link?.message} />
                  </div>
                  <div className="md:col-span-3">
                    <Field label="Texto livre">
                      <Textarea {...register(`referencias.${i}.textoLivre`)} rows={2} placeholder="Para o que não couber nos campos acima" />
                    </Field>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={isSubmitting || salvar.isPending}>
          {salvar.isPending ? "Salvando…" : editando ? "Salvar alterações" : "Cadastrar planta"}
        </Button>
        <Link to={voltarPara}>
          <Button type="button" className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50">
            Cancelar
          </Button>
        </Link>
      </div>
    </form>
  );
}
