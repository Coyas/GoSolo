#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

fail() {
    echo ""
    echo "ERRO: $1"
    echo "A parar serviços em execução..."
    docker compose -f "$ROOT/docker-compose.yml" down 2>/dev/null || true
    exit 1
}

echo "=== Testes backend ==="
cd "$ROOT/gosolo"
./gradlew test --no-daemon || fail "Testes backend falharam."

echo ""
echo "=== Testes frontend ==="
cd "$ROOT/wgosolo"
npm test -- --watchAll=false || fail "Testes frontend falharam."

echo ""
echo "=== Todos os testes passaram. A iniciar serviços... ==="
echo ""
cd "$ROOT"
docker compose up --build "$@"
