import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Leaf, Plus, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Input, Select } from "@/components/ui";
import { listarPlantas, mensagemDeErro, plantasKeys } from "@/services/plantasApi";

const TAMANHO_PAGINA = 20;
const NOMES_VISIVEIS = 2;

type Situacao = "" | "true" | "false";

export function RepositorioPage() {
  const [busca, setBusca] = useState("");
  const [buscaAplicada, setBuscaAplicada] = useState("");
  const [familia, setFamilia] = useState("");
  const [familiaAplicada, setFamiliaAplicada] = useState("");
  const [situacao, setSituacao] = useState<Situacao>("true");
  const [pagina, setPagina] = useState(0);

  // Debounce dos campos de texto: a API só é chamada 300 ms após parar de digitar.
  useEffect(() => {
    const t = setTimeout(() => {
      setBuscaAplicada(busca.trim());
      setFamiliaAplicada(familia.trim());
      setPagina(0);
    }, 300);
    return () => clearTimeout(t);
  }, [busca, familia]);

  const filtro = {
    busca: buscaAplicada || undefined,
    familiaBotanica: familiaAplicada || undefined,
    ativo: situacao === "" ? undefined : situacao === "true",
    page: pagina,
    size: TAMANHO_PAGINA,
  };

  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: plantasKeys.lista(filtro),
    queryFn: () => listarPlantas(filtro),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Repositório de plantas</h1>
          <p className="text-sm text-slate-500">
            Fichas botânicas com nomes populares, uso, similares e referências. Dados reais da API.
          </p>
        </div>
        <Link to="/repositorio/nova">
          <Button>
            <Plus size={17} />
            Nova planta
          </Button>
        </Link>
      </div>

      <Card className="grid gap-3 md:grid-cols-[1fr_220px_200px]">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-3 text-slate-400" size={17} />
          <div className="[&>input]:pl-9">
            <Input
              placeholder="Buscar por nome científico ou popular"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              aria-label="Buscar planta"
            />
          </div>
        </div>
        <Input
          placeholder="Família botânica"
          value={familia}
          onChange={(e) => setFamilia(e.target.value)}
          aria-label="Filtrar por família botânica"
        />
        <Select
          value={situacao}
          onChange={(e) => {
            setSituacao(e.target.value as Situacao);
            setPagina(0);
          }}
          aria-label="Filtrar por situação cadastral"
        >
          <option value="true">Ativas</option>
          <option value="false">Arquivadas</option>
          <option value="">Todas</option>
        </Select>
      </Card>

      {isPending && (
        <Card>
          <p className="text-sm text-slate-500">Carregando plantas…</p>
        </Card>
      )}

      {isError && (
        <Card className="border-red-200">
          <p className="font-semibold text-red-800">Não foi possível carregar o repositório.</p>
          <p className="my-2 text-sm text-slate-600">{mensagemDeErro(error)}</p>
          <Button onClick={() => refetch()}>Tentar novamente</Button>
        </Card>
      )}

      {data && data.content.length === 0 && (
        <Card className="text-center">
          <Leaf className="mx-auto mb-2 text-emerald-700" />
          <p className="font-semibold">Nenhuma planta encontrada.</p>
          <p className="text-sm text-slate-500">
            {buscaAplicada || familiaAplicada
              ? "Ajuste a busca ou os filtros."
              : "Cadastre a primeira planta do repositório."}
          </p>
        </Card>
      )}

      {data && data.content.length > 0 && (
        <Card className={`p-0 ${isFetching ? "opacity-70" : ""}`}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-stone-200 text-left text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-semibold">Nome científico</th>
                  <th className="px-5 py-3 font-semibold">Nomes populares</th>
                  <th className="px-5 py-3 font-semibold">Família</th>
                  <th className="px-5 py-3 text-right font-semibold">Similares</th>
                  <th className="px-5 py-3 font-semibold">Situação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {data.content.map((p) => {
                  const visiveis = p.nomesPopulares.slice(0, NOMES_VISIVEIS);
                  const extras = p.nomesPopulares.length - visiveis.length;
                  return (
                    <tr key={p.id} className="hover:bg-stone-50">
                      <td className="px-5 py-3">
                        <Link
                          to={`/repositorio/${p.id}`}
                          className="font-semibold italic text-emerald-900 hover:underline"
                        >
                          {p.nomeCientifico}
                        </Link>
                      </td>
                      <td className="px-5 py-3">
                        {visiveis.length === 0 ? (
                          <span className="text-slate-400">—</span>
                        ) : (
                          <span>
                            {visiveis.join(", ")}
                            {extras > 0 && (
                              <span className="ml-1 text-xs font-semibold text-slate-500">
                                +{extras}
                              </span>
                            )}
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-3">{p.familiaBotanica ?? <span className="text-slate-400">—</span>}</td>
                      <td className="px-5 py-3 text-right tabular-nums">{p.quantidadeSimilares}</td>
                      <td className="px-5 py-3">
                        <Badge tone={p.ativo ? "good" : "neutral"}>{p.ativo ? "Ativa" : "Arquivada"}</Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-stone-200 px-5 py-3 text-sm text-slate-600">
            <span>
              Página {data.page + 1} de {Math.max(data.totalPages, 1)} · {data.totalElements}{" "}
              {data.totalElements === 1 ? "planta" : "plantas"}
            </span>
            <div className="flex gap-2">
              <Button
                className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50"
                disabled={data.first}
                onClick={() => setPagina((p) => Math.max(0, p - 1))}
              >
                Anterior
              </Button>
              <Button
                className="bg-white text-emerald-900 ring-1 ring-stone-300 hover:bg-stone-50"
                disabled={data.last}
                onClick={() => setPagina((p) => p + 1)}
              >
                Próxima
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
