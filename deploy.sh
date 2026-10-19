#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven (mvn) is required on PATH." >&2
  exit 1
fi

echo "[INFO] Building ORSV0 from current source and running tests..."
mvn clean test package

if [[ ! -f "target/ORSV0.war" ]]; then
  echo "[ERROR] target/ORSV0.war was not created." >&2
  exit 1
fi

echo "[INFO] Built target/ORSV0.war (Build ID: ORS-2.1.1-RELEASE-20261015)."

if [[ "${1:-}" == "--deploy" ]]; then
  if [[ -z "${TOMCAT_HOME:-}" ]]; then
    echo "[ERROR] TOMCAT_HOME must be set before deploying." >&2
    exit 1
  fi
  if [[ ! -d "$TOMCAT_HOME/webapps" ]]; then
    echo "[ERROR] $TOMCAT_HOME/webapps does not exist." >&2
    exit 1
  fi
  rm -f "$TOMCAT_HOME/webapps/ORSV0.war"
  rm -rf "$TOMCAT_HOME/webapps/ORSV0"
  cp "target/ORSV0.war" "$TOMCAT_HOME/webapps/ORSV0.war"
  echo "[INFO] Deployed target/ORSV0.war to $TOMCAT_HOME/webapps/ORSV0.war"
fi
