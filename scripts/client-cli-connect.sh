#!/bin/bash

source "./scripts/utils.sh"

function main() {
    local client_container="$(docker ps | grep 'app-client')"

    if [[ -z "$client_container" ]]; then
      echo "[INFO] staring app-client container in interactive mode"
      docker start -ai docker-app-client-1
      return 0
    fi

    echo "[INFO] Attaching to running app-client container in interactive mode. Type 'help' to show info"
    docker attach docker-app-client-1
}

(main "$@")