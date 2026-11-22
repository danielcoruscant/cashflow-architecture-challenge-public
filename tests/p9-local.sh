#!/bin/sh
# Execução local: não inicia Cosmos Emulator, Service Bus ou Azure Functions.
set -eu
cd "$(dirname "$0")/.."

for pom in \
  services/lancamentos-service/pom.xml \
  services/consolidado-service/pom.xml \
  integrations/cdc-publisher/function/cdc-function/pom.xml \
  integrations/cdc-consumer/pom.xml
do
  mvn -o -q -f "$pom" clean test jacoco:report
done

python3 tests/p9-coverage.py
