#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${ROOT_DIR}/.run/pids"

if [[ ! -d "${PID_DIR}" ]]; then
  echo "No PID directory found: ${PID_DIR}"
  exit 0
fi

is_pid_alive() {
  local pid="$1"
  [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1
}

stop_one() {
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
      echo "Force stopping ${name} (pid ${pid})"
      kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
  else
    echo "${name} is not running"
  fi
  rm -f "${pid_file}"
}

for name in finance-streamlit finance-web document-rag mcp-gateway browser-mcp web-reader-mcp zai-mcp finance-mcp finance-provider; do
  stop_one "${name}"
done

echo "Stopped known Finance General Agent services."
