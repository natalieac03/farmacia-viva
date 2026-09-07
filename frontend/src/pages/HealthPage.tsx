import { useQuery } from "@tanstack/react-query";
import { Activity, AlertTriangle, Loader2, RefreshCw } from "lucide-react";
import { fetchHealth } from "@/lib/health";
import { cn } from "@/lib/utils";

export function HealthPage() {
  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: ["health"],
    queryFn: fetchHealth,
    refetchInterval: 30_000,
  });

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <section className="w-full max-w-md rounded-lg border bg-card p-8 shadow-sm">
        <header className="mb-6">
          <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
            CREMIC · Farmácia Viva
          </p>
          <h1 className="mt-1 text-2xl font-semibold text-foreground">
            Situação do sistema
          </h1>
        </header>

        {isPending && (
          <div className="flex items-center gap-3 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
            <span>Consultando o backend</span>
          </div>
        )}

        {isError && (
          <div className="rounded-md border border-destructive/30 bg-destructive/10 p-4">
            <div className="flex items-center gap-2 font-medium text-destructive">
              <AlertTriangle className="h-5 w-5" aria-hidden="true" />
              Backend indisponível
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              {error instanceof Error
                ? error.message
                : "Não foi possível consultar o serviço."}
            </p>
          </div>
        )}

        {data && (
          <dl className="space-y-4">
            <div className="flex items-center justify-between">
              <dt className="text-sm text-muted-foreground">Status</dt>
              <dd
                className={cn(
                  "inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-medium",
                  data.status === "UP"
                    ? "bg-accent text-accent-foreground"
                    : "bg-destructive/10 text-destructive",
                )}
              >
                <Activity className="h-4 w-4" aria-hidden="true" />
                {data.status}
              </dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-sm text-muted-foreground">Aplicação</dt>
              <dd className="text-sm font-medium">{data.application}</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-sm text-muted-foreground">Verificado em</dt>
              <dd className="text-sm font-medium">
                {new Date(data.timestamp).toLocaleString("pt-BR")}
              </dd>
            </div>
          </dl>
        )}

        <footer className="mt-8">
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            className="inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:opacity-60"
          >
            <RefreshCw
              className={cn("h-4 w-4", isFetching && "animate-spin")}
              aria-hidden="true"
            />
            Verificar novamente
          </button>
        </footer>
      </section>
    </main>
  );
}
