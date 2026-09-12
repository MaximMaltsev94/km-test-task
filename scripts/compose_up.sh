#!/bin/bash

source "./scripts/utils.sh"

function main() {
  local compose_file="./docker/docker-compose.yaml"
  docker compose --file "$compose_file" up -d --force-recreate || die "Error while running docker compose"
}

(main "$@")