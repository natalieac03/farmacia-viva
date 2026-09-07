#!/usr/bin/env bash
#
# Sobe o ambiente completo da Farmacia Viva (Postgres + backend + frontend)
# com um unico comando, a partir de um unico terminal.
#
# Uso:
#   ./start.sh
#
# Para parar tudo: Ctrl+C neste terminal.
#
# Logs ficam em .run/backend.log e .run/frontend.log (tambem espelhados
# aqui no terminal).

set -euo pipefail
cd "$(dirname "$0")"

mkdir -p .run

# Carrega o SDKMAN (Java/Maven) e o nvm (Node), caso tenham sido instalados
# via a instalacao padrao de cada um. Se voce usa outro gerenciador (asdf,
# apt, etc), pode remover ou ajustar estas duas linhas.
set +u
[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ] && source "$HOME/.sdkman/bin/sdkman-init.sh"
[ -s "$HOME/.nvm/nvm.sh" ] && source "$HOME/.nvm/nvm.sh" && nvm use --silent default >/dev/null 2>&1 || true
set -u

PIDS=()

encerrar() {
    echo ""
    echo "Encerrando..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" >/dev/null 2>&1 || true
    done
    wait >/dev/null 2>&1 || true
    echo "Tudo parado."
}
trap encerrar EXIT INT TERM

echo "==> Subindo PostgreSQL (Docker)..."
docker compose up -d postgres

echo "==> Aguardando o PostgreSQL ficar pronto..."
until docker compose exec -T postgres pg_isready -U farmacia -d farmacia_viva >/dev/null 2>&1; do
    sleep 1
done
echo "    Postgres pronto."

echo "==> Subindo backend (Spring Boot, profile dev)..."
(
    cd backend
    mvn -q spring-boot:run -Dspring-boot.run.profiles=dev
) > .run/backend.log 2>&1 &
PIDS+=($!)

echo "==> Aguardando o backend responder em http://localhost:8080/api/v1/health ..."
until curl -sf http://localhost:8080/api/v1/health >/dev/null 2>&1; do
    sleep 1
done
echo "    Backend pronto."

echo "==> Subindo frontend (Vite)..."
(
    cd frontend
    if [ ! -d node_modules ]; then
        npm install
    fi
    npm run dev -- --host
) > .run/frontend.log 2>&1 &
PIDS+=($!)

echo ""
echo "=================================================="
echo " Backend:  http://localhost:8080/api/v1/health"
echo " Frontend: http://localhost:5173"
echo " Logs:     tail -f .run/backend.log .run/frontend.log"
echo " Ctrl+C aqui para derrubar tudo."
echo "=================================================="
echo ""

tail -f .run/backend.log .run/frontend.log &
TAIL_PID=$!
PIDS+=("$TAIL_PID")

wait "$TAIL_PID"
