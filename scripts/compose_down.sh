#!/bin/bash

source "./scripts/utils.sh"

function main() {
  local compose_file="./docker/docker-compose.yaml"
  docker compose --file "$compose_file" down || die "Error while stopping docker compose"
}

(main "$@")