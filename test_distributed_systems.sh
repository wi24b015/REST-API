#!/usr/bin/env bash
set -euo pipefail

PROJECT="/c/Users/flori/Git_Repositorys/semesterprojekt"
LOG_DIR="$PROJECT/test-logs"

API_URL="http://localhost:8080"
RABBIT_API="http://localhost:15672/api/queues/%2F/energy.messages"
START_LIVE="${1:-}"

mkdir -p "$LOG_DIR"


start_live_environment() {
  echo
  echo "== Starte Live-Umgebung über native Windows-Datei =="
  cmd.exe /c "C:\\Users\\flori\\Git_Repositorys\\semesterprojekt\\start_all.cmd"
  echo "Live-Umgebung wurde gestartet."
}

echo "== Prüfe Tools =="
command -v docker >/dev/null || { echo "docker fehlt"; exit 1; }
command -v mvn >/dev/null || { echo "mvn fehlt"; exit 1; }
command -v curl >/dev/null || { echo "curl fehlt"; exit 1; }

cd "$PROJECT"

cleanup() {
  echo
  echo "== Stoppe gestartete Spring Boot Prozesse =="
  [[ -n "${API_PID:-}" ]] && kill "$API_PID" 2>/dev/null || true
  [[ -n "${USAGE_PID:-}" ]] && kill "$USAGE_PID" 2>/dev/null || true
  [[ -n "${CURRENT_PID:-}" ]] && kill "$CURRENT_PID" 2>/dev/null || true
  [[ -n "${PRODUCER_PID:-}" ]] && kill "$PRODUCER_PID" 2>/dev/null || true
  [[ -n "${USER_PID:-}" ]] && kill "$USER_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo
echo "== Starte Docker Infrastruktur =="
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps

echo
echo "== Warte auf RabbitMQ Management =="
for i in {1..60}; do
  if curl -fsu guest:guest "http://localhost:15672/api/overview" >/dev/null; then
    echo "RabbitMQ ist erreichbar."
    break
  fi
  sleep 2
  if [[ "$i" -eq 60 ]]; then
    echo "RabbitMQ wurde nicht erreichbar."
    exit 1
  fi
done

echo
echo "== Führe Maven Tests aus =="
(
  cd "$PROJECT/energyapi"
  mvn test
)

(
  cd "$PROJECT/energy-producer"
  mvn test
)

(
  cd "$PROJECT/energy-user"
  mvn test
)

(
  cd "$PROJECT/usage-service"
  mvn test
)

(
  cd "$PROJECT/current-percentage-service"
  mvn test
)

if [[ "$START_LIVE" == "--live" ]]; then
  start_live_environment
  trap - EXIT
  exit 0
fi

echo
echo "== Starte API =="
(
  cd "$PROJECT/energyapi"
  mvn spring-boot:run > "$LOG_DIR/energyapi.log" 2>&1
) &
API_PID=$!

echo "API PID: $API_PID"

echo
echo "== Warte auf API =="
for i in {1..90}; do
  if curl -fs "$API_URL/energy/current" >/dev/null; then
    echo "API ist erreichbar."
    break
  fi
  sleep 2
  if [[ "$i" -eq 90 ]]; then
    echo "API wurde nicht erreichbar. Log:"
    tail -n 80 "$LOG_DIR/energyapi.log"
    exit 1
  fi
done

echo
echo "== Starte Usage Service und Current Percentage Service =="
(
  cd "$PROJECT/usage-service"
  mvn spring-boot:run > "$LOG_DIR/usage-service.log" 2>&1
) &
USAGE_PID=$!

(
  cd "$PROJECT/current-percentage-service"
  mvn spring-boot:run > "$LOG_DIR/current-percentage-service.log" 2>&1
) &
CURRENT_PID=$!

echo "Usage Service PID: $USAGE_PID"
echo "Current Percentage Service PID: $CURRENT_PID"

echo
echo "== Warte auf Consumer-Services =="
sleep 12

echo
echo "== Starte Producer und User =="
(
  cd "$PROJECT/energy-producer"
  mvn spring-boot:run > "$LOG_DIR/energy-producer.log" 2>&1
) &
PRODUCER_PID=$!

(
  cd "$PROJECT/energy-user"
  mvn spring-boot:run > "$LOG_DIR/energy-user.log" 2>&1
) &
USER_PID=$!

echo "Producer PID: $PRODUCER_PID"
echo "User PID: $USER_PID"

echo
echo "== Sammle Messages für 20 Sekunden =="
sleep 20

echo
echo "== Prüfe REST API: current =="
CURRENT_RESPONSE="$(curl -fs "$API_URL/energy/current")"
echo "$CURRENT_RESPONSE"

echo
echo "== Prüfe REST API: historical =="
START="$(date -u +"%Y-%m-%dT00:00:00")"
END="$(date -u -d tomorrow +"%Y-%m-%dT00:00:00" 2>/dev/null || date -u -v+1d +"%Y-%m-%dT00:00:00")"

HISTORICAL_RESPONSE="$(curl -fs "$API_URL/energy/historical?start=$START&end=$END")"
echo "$HISTORICAL_RESPONSE"

echo
echo "== Prüfe RabbitMQ Queue =="
curl -fsu guest:guest "$RABBIT_API" | sed 's/,/\n/g' | grep -E '"name"|"messages"|"messages_ready"|"messages_unacknowledged"|"consumers"' || true

echo
echo "== Prüfe Logs auf gesendete Messages =="
grep -ai "Processed message" "$LOG_DIR/usage-service.log" | tail -n 5 || {
  echo "Keine verarbeiteten Messages im Usage-Service-Log gefunden."
  tail -n 80 "$LOG_DIR/usage-service.log"
  exit 1
}

grep -ai "Updated current percentage" "$LOG_DIR/current-percentage-service.log" | tail -n 5 || {
  echo "Keine Updates im Current-Percentage-Service-Log gefunden."
  tail -n 80 "$LOG_DIR/current-percentage-service.log"
  exit 1
}

grep -ai "Sent producer message" "$LOG_DIR/energy-producer.log" | tail -n 5 || {
  echo "Keine Producer-Messages im Log gefunden."
  tail -n 80 "$LOG_DIR/energy-producer.log"
  exit 1
}

grep -ai "Sent user message" "$LOG_DIR/energy-user.log" | tail -n 5 || {
  echo "Keine User-Messages im Log gefunden."
  tail -n 80 "$LOG_DIR/energy-user.log"
  exit 1
}

echo
echo "== Ergebnis =="
echo "Funktionstest erfolgreich."
echo "Logs liegen unter: $LOG_DIR"

echo
if [[ "$START_LIVE" != "--live" ]]; then
  read -r -p "Live-Terminals mit API, Producer, User und GUI starten? (j/N): " START_LIVE || START_LIVE="N"
fi

if [[ "$START_LIVE" == "--live" || "$START_LIVE" =~ ^[JjYy]$ ]]; then
  cleanup
  trap - EXIT
  start_live_environment
fi
