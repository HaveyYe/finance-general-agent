#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/.run"
LOG_DIR="${RUN_DIR}/logs"
PID_DIR="${RUN_DIR}/pids"

DO_BUILD=1
DO_NPM_INSTALL=1
INSTALL_BROWSER=0
RUN_SMOKE=0
FORCE_RESTART=0

usage() {
  cat <<'USAGE'
Usage: scripts/start-all.sh [options]

Options:
  --no-build            Skip Maven package step.
  --skip-npm-install    Skip npm install checks.
  --install-browser     Run Playwright Chromium install before Browser MCP startup.
  --smoke               Run MCP smoke tests after startup.
  --force               Stop known local services first, then start again.
  -h, --help            Show this help.

Common:
  scripts/start-all.sh
  scripts/start-all.sh --force --smoke
  AGENT_MODE=llm GLM_API_KEY=your-key scripts/start-all.sh
  Z_AI_API_KEY=your-key scripts/start-all.sh
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build) DO_BUILD=0 ;;
    --skip-npm-install) DO_NPM_INSTALL=0 ;;
    --install-browser) INSTALL_BROWSER=1 ;;
    --smoke) RUN_SMOKE=1 ;;
    --force) FORCE_RESTART=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

mkdir -p "${LOG_DIR}" "${PID_DIR}"

load_env_file() {
  local env_file="$1"
  [[ -f "${env_file}" ]] || return 0
  echo "Loading environment: ${env_file#${ROOT_DIR}/}"
  set -a
  # shellcheck disable=SC1090
  source "${env_file}"
  set +a
}

trim_value() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

apollo_property_raw() {
  local properties_file="$1"
  local key="$2"
  awk -v key="${key}" '
    /^[[:space:]]*($|#|!)/ { next }
    {
      line = $0
      sub(/^[[:space:]]+/, "", line)
      separator = index(line, "=")
      if (!separator) separator = index(line, ":")
      if (!separator) next
      name = substr(line, 1, separator - 1)
      value = substr(line, separator + 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      if (name == key) found = value
    }
    END { if (found != "") print found }
  ' "${properties_file}"
}

apollo_property() {
  local properties_file="$1"
  local key="$2"
  local raw
  raw="$(apollo_property_raw "${properties_file}" "${key}")"
  raw="$(trim_value "${raw}")"
  if [[ "${raw}" =~ ^\$\{([^:}]+):(.+)\}$ ]]; then
    printf '%s' "${BASH_REMATCH[2]}"
    return 0
  fi
  if [[ "${raw}" =~ ^\$\{([^}]+)\}$ ]]; then
    apollo_property "${properties_file}" "${BASH_REMATCH[1]}"
    return 0
  fi
  printf '%s' "${raw}"
}

first_apollo_property() {
  local properties_file="$1"
  shift
  local key value
  for key in "$@"; do
    value="$(apollo_property "${properties_file}" "${key}")"
    if [[ -n "${value}" ]]; then
      printf '%s' "${value}"
      return 0
    fi
  done
}

export_if_unset() {
  local name="$1"
  local value="$2"
  [[ -n "${value}" ]] || return 0
  if [[ -z "${!name:-}" ]]; then
    export "${name}=${value}"
  fi
}

normalize_dubbo_registry() {
  local registry="$1"
  local protocol="${2:-zookeeper}"
  registry="$(trim_value "${registry}")"
  [[ -n "${registry}" && "${registry}" != "N/A" ]] || return 0
  if [[ "${registry}" == *"://"* ]]; then
    printf '%s' "${registry}"
  else
    printf '%s://%s' "${protocol}" "${registry}"
  fi
}

load_apollo_properties_file() {
  local properties_file="$1"
  [[ -f "${properties_file}" ]] || return 0
  echo "Loading Apollo Dubbo properties: ${properties_file#${ROOT_DIR}/}"

  local app_name timeout version reference_env registry protocol zk_host zk_port
  app_name="$(first_apollo_property "${properties_file}" dubbo.application.name dubbo.name application.name spring.application.name)"
  timeout="$(first_apollo_property "${properties_file}" dubbo.consumer.timeout dubbo.timeout dubbo.read.timeout dubbo.connect.timeout)"
  version="$(first_apollo_property "${properties_file}" dubbo.reference.version dubbo.consumer.version dubbo.version)"
  reference_env="$(first_apollo_property "${properties_file}" dubbo.reference.env dubbo.env zcy.dubbo.env)"
  protocol="$(first_apollo_property "${properties_file}" dubbo.registry.protocol)"
  protocol="${protocol:-zookeeper}"

  registry="$(first_apollo_property "${properties_file}" dubbo.registry.address dubbo.registry zookeeper.cluster)"
  if [[ -z "${registry}" ]]; then
    zk_host="$(first_apollo_property "${properties_file}" zookeeper.host curator.zk.host)"
    zk_port="$(first_apollo_property "${properties_file}" zookeeper.port curator.zk.port)"
    if [[ -n "${zk_host}" && -n "${zk_port}" ]]; then
      registry="${zk_host}:${zk_port}"
    fi
  fi
  registry="$(normalize_dubbo_registry "${registry}" "${protocol}")"

  export_if_unset DUBBO_APPLICATION_NAME "${app_name}"
  export_if_unset FINANCE_DUBBO_TIMEOUT_MS "${timeout}"
  export_if_unset FINANCE_DUBBO_VERSION "${version}"
  export_if_unset FINANCE_DUBBO_REFERENCE_ENV "${reference_env}"
  export_if_unset FINANCE_DUBBO_REGISTRY "${registry}"
  export_if_unset ZCY_ENV_LABEL "$(first_apollo_property "${properties_file}" zcy.env.label)"
}

load_env_file "${ROOT_DIR}/.env"
load_env_file "${ROOT_DIR}/.env.local"
load_apollo_properties_file "${ROOT_DIR}/apollo.properties"
load_apollo_properties_file "${ROOT_DIR}/apollo.local.properties"
load_apollo_properties_file "${ROOT_DIR}/.apollo.properties"
load_apollo_properties_file "${ROOT_DIR}/.apollo.local.properties"

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}" >&2
    exit 1
  fi
}

port_pid() {
  local port="$1"
  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

is_pid_alive() {
  local pid="$1"
  [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1
}

stop_pid_file() {
  local name="$1"
  local pid_file="${PID_DIR}/${name}.pid"
  [[ -f "${pid_file}" ]] || return 0
  local pid
  pid="$(cat "${pid_file}" 2>/dev/null || true)"
  if is_pid_alive "${pid}"; then
    echo "Stopping ${name} (pid ${pid})"
    kill "${pid}" >/dev/null 2>&1 || true
    for _ in {1..30}; do
      is_pid_alive "${pid}" || break
      sleep 0.2
    done
    if is_pid_alive "${pid}"; then
      kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
  fi
  rm -f "${pid_file}"
}

stop_known_services() {
  for name in finance-web document-rag mcp-gateway browser-mcp web-reader-mcp zai-mcp finance-mcp finance-provider; do
    stop_pid_file "${name}"
  done
}

wait_port() {
  local port="$1"
  local name="$2"
  local log_file="$3"
  for _ in {1..120}; do
    if [[ -n "$(port_pid "${port}")" ]]; then
      return 0
    fi
    sleep 0.5
  done
  echo "${name} did not listen on port ${port}. Log: ${log_file}" >&2
  tail -n 80 "${log_file}" >&2 || true
  exit 1
}

wait_http() {
  local url="$1"
  local name="$2"
  local log_file="$3"
  for _ in {1..120}; do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  echo "${name} is not ready at ${url}. Log: ${log_file}" >&2
  tail -n 80 "${log_file}" >&2 || true
  exit 1
}

wait_mcp() {
  local url="$1"
  local name="$2"
  local log_file="$3"
  for _ in {1..120}; do
    if curl -fsS "${url}" \
      -H 'Content-Type: application/json' \
      -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  echo "${name} is not ready at ${url}. Log: ${log_file}" >&2
  tail -n 80 "${log_file}" >&2 || true
  exit 1
}

detect_provider_direct_url() {
  local log_file="$1"
  local export_url
  for _ in {1..60}; do
    export_url="$(sed -n 's/.*Export dubbo service com\.zcy\.finance\.api\.ArDashboardService to url \(dubbo:\/\/[^ ]*\).*/\1/p' "${log_file}" | tail -n 1)"
    if [[ "${export_url}" =~ ^dubbo://([^:/]+):([0-9]+) ]]; then
      echo "dubbo://${BASH_REMATCH[1]}:${BASH_REMATCH[2]}"
      return 0
    fi
    sleep 0.5
  done
  echo "dubbo://127.0.0.1:20880"
}

detect_local_dubbo_host() {
  local host
  host="$(ipconfig getifaddr en0 2>/dev/null || true)"
  if [[ -n "${host}" ]]; then
    echo "${host}"
    return 0
  fi
  host="$(ifconfig 2>/dev/null | awk '/^[a-z0-9]+:/{iface=$1} /inet / && $2 !~ /^127\\./ && $2 !~ /^198\\.18\\./ {print $2; exit}')"
  if [[ -n "${host}" ]]; then
    echo "${host}"
    return 0
  fi
  echo ""
}

start_bg() {
  local name="$1"
  local workdir="$2"
  local port="$3"
  shift 3
  local pid_file="${PID_DIR}/${name}.pid"
  local log_file="${LOG_DIR}/${name}.log"
  local existing_pid
  existing_pid="$(port_pid "${port}")"
  if [[ -n "${existing_pid}" ]]; then
    echo "${name} already listening on port ${port} (pid ${existing_pid}), skip start."
    echo "${existing_pid}" > "${pid_file}"
    return 0
  fi
  echo "Starting ${name} on port ${port} ..."
  (
    cd "${workdir}"
    nohup "$@" > "${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )
}

npm_install_if_needed() {
  local dir="$1"
  if [[ "${DO_NPM_INSTALL}" -eq 0 ]]; then
    return 0
  fi
  if [[ ! -d "${dir}/node_modules" ]]; then
    echo "Installing npm dependencies: ${dir#${ROOT_DIR}/}"
    (cd "${dir}" && npm install)
  fi
}

require_cmd mvn
require_cmd java
require_cmd npm
require_cmd node
require_cmd curl
require_cmd lsof

if [[ "${RUN_SMOKE}" -eq 1 ]]; then
  require_cmd jq
fi

if [[ "${FORCE_RESTART}" -eq 1 ]]; then
  stop_known_services
fi

if [[ "${DO_BUILD}" -eq 1 ]]; then
  echo "Building Java modules ..."
  (cd "${ROOT_DIR}" && mvn -q -DskipTests package)
fi

npm_install_if_needed "${ROOT_DIR}/mcp/zai-mcp-server"
npm_install_if_needed "${ROOT_DIR}/mcp/web-reader-server"
npm_install_if_needed "${ROOT_DIR}/mcp/integrated-browser-server"
npm_install_if_needed "${ROOT_DIR}/mcp/document-rag-server"
npm_install_if_needed "${ROOT_DIR}/mcp/mcp-gateway"
npm_install_if_needed "${ROOT_DIR}/finance-web"

if [[ "${INSTALL_BROWSER}" -eq 1 ]]; then
  echo "Installing Playwright Chromium ..."
  (cd "${ROOT_DIR}/mcp/integrated-browser-server" && npx playwright install chromium)
fi

DUBBO_PROXY_ENV_UNSET=(-u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u http_proxy -u https_proxy -u all_proxy)
DUBBO_JAVA_TOOL_OPTIONS="${DUBBO_JAVA_TOOL_OPTIONS:--Djava.net.useSystemProxies=false -Dhttp.proxyHost= -Dhttps.proxyHost= -DsocksProxyHost= -Dhttp.nonProxyHosts=*}"

FINANCE_PROVIDER_HOST="${FINANCE_DUBBO_HOST:-${DUBBO_IP_TO_BIND:-$(detect_local_dubbo_host)}}"
if [[ -n "${FINANCE_PROVIDER_HOST}" ]]; then
  echo "Using finance Dubbo provider host: ${FINANCE_PROVIDER_HOST}"
  start_bg finance-provider "${ROOT_DIR}" 20880 env "${DUBBO_PROXY_ENV_UNSET[@]}" JAVA_TOOL_OPTIONS="${DUBBO_JAVA_TOOL_OPTIONS}" DUBBO_IP_TO_BIND="${FINANCE_PROVIDER_HOST}" DUBBO_IP_TO_REGISTRY="${FINANCE_PROVIDER_HOST}" java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
else
  start_bg finance-provider "${ROOT_DIR}" 20880 env "${DUBBO_PROXY_ENV_UNSET[@]}" JAVA_TOOL_OPTIONS="${DUBBO_JAVA_TOOL_OPTIONS}" java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
fi
wait_port 20880 finance-provider "${LOG_DIR}/finance-provider.log"

FINANCE_PROVIDER_DIRECT_URL="${FINANCE_DUBBO_DIRECT_URL:-}"
if [[ -z "${FINANCE_PROVIDER_DIRECT_URL}" ]]; then
  FINANCE_PROVIDER_DIRECT_URL="$(detect_provider_direct_url "${LOG_DIR}/finance-provider.log")"
fi
if [[ -n "${FINANCE_PROVIDER_DIRECT_URL}" ]]; then
  echo "Using finance Dubbo direct URL: ${FINANCE_PROVIDER_DIRECT_URL}"
else
  echo "Using finance Dubbo registry: ${FINANCE_DUBBO_REGISTRY:-}"
fi
if [[ "${DO_BUILD}" -eq 1 ]]; then
  echo "Installing finance-api for finance-mcp runtime ..."
  (cd "${ROOT_DIR}/finance-service" && mvn -q -N -DskipTests install)
  (cd "${ROOT_DIR}" && mvn -q -pl finance-service/finance-api -DskipTests install)
fi
if [[ -n "${FINANCE_PROVIDER_DIRECT_URL}" ]]; then
  start_bg finance-mcp "${ROOT_DIR}" 8086 env "${DUBBO_PROXY_ENV_UNSET[@]}" -u DUBBO_IP_TO_BIND -u DUBBO_IP_TO_REGISTRY JAVA_TOOL_OPTIONS="${DUBBO_JAVA_TOOL_OPTIONS}" MCP_SERVER_PORT=8086 FINANCE_MCP_SAFETY_WRITE_ENABLED=true FINANCE_DUBBO_DIRECT_URL="${FINANCE_PROVIDER_DIRECT_URL}" mvn -pl mcp/finance-mcp spring-boot:run
else
  start_bg finance-mcp "${ROOT_DIR}" 8086 env "${DUBBO_PROXY_ENV_UNSET[@]}" -u DUBBO_IP_TO_BIND -u DUBBO_IP_TO_REGISTRY -u FINANCE_DUBBO_DIRECT_URL JAVA_TOOL_OPTIONS="${DUBBO_JAVA_TOOL_OPTIONS}" MCP_SERVER_PORT=8086 FINANCE_MCP_SAFETY_WRITE_ENABLED=true mvn -pl mcp/finance-mcp spring-boot:run
fi
wait_mcp http://localhost:8086/mcp finance-mcp "${LOG_DIR}/finance-mcp.log"

start_bg zai-mcp "${ROOT_DIR}/mcp/zai-mcp-server" 8088 npm run dev
wait_mcp http://localhost:8088/mcp zai-mcp "${LOG_DIR}/zai-mcp.log"

start_bg web-reader-mcp "${ROOT_DIR}/mcp/web-reader-server" 8089 npm run dev
wait_mcp http://localhost:8089/mcp web-reader-mcp "${LOG_DIR}/web-reader-mcp.log"

start_bg browser-mcp "${ROOT_DIR}/mcp/integrated-browser-server" 8090 npm run dev
wait_mcp http://localhost:8090/mcp browser-mcp "${LOG_DIR}/browser-mcp.log"

start_bg document-rag "${ROOT_DIR}/mcp/document-rag-server" 8091 npm run dev
wait_http http://localhost:8091/knowledge/health document-rag "${LOG_DIR}/document-rag.log"

start_bg mcp-gateway "${ROOT_DIR}/mcp/mcp-gateway" 9000 npm run dev
wait_http http://localhost:9000/health mcp-gateway "${LOG_DIR}/mcp-gateway.log"

start_bg finance-web "${ROOT_DIR}/finance-web" 5173 npm run dev
wait_http http://localhost:5173 finance-web "${LOG_DIR}/finance-web.log"

if [[ "${RUN_SMOKE}" -eq 1 ]]; then
  echo "Running smoke tests ..."
  (cd "${ROOT_DIR}" && bash mcp/scripts/smoke-test.sh)
  (cd "${ROOT_DIR}" && bash mcp/scripts/web-reader-smoke-test.sh)
fi

cat <<EOF2

Finance General Agent started.

URLs:
  Web:          http://localhost:5173
  DingTalk sim: http://localhost:5173/chat?channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
  Gateway:      http://localhost:9000
  Gateway MCP:  http://localhost:9000/mcp
  Knowledge:    http://localhost:8091/knowledge

Logs:
  ${LOG_DIR}

Stop:
  scripts/stop-all.sh
EOF2
