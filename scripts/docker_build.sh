#!/bin/bash

source "./scripts/utils.sh"

function build_gradle () {
  ./gradlew clean build || return 1
}

function build_docker_client_app() {
  mkdir -p "$DOCKER_BUILD_WORKDIR" || return 1
  cp "$CLIENT_JAR_PATH" "$DOCKER_BUILD_WORKDIR/client.jar" || return 1
  docker build -f "docker/Dockerfile.client" -t km-test-task-client:latest docker || return 1
}

function build_docker_server_app() {
  mkdir -p "$DOCKER_BUILD_WORKDIR" || return 1
  cp "$SERVER_JAR_PATH" "$DOCKER_BUILD_WORKDIR/server.jar" || return 1
  docker build -f "docker/Dockerfile.server" -t km-test-task-server:latest docker || return 1
}

function build_docker_db_migration() {
  mkdir -p "$DOCKER_BUILD_WORKDIR/liquibase" || return 1
  cp -r "$LIQUIBASE_CHANGELOGS_DIR" "$DOCKER_BUILD_WORKDIR/liquibase" || return 1
  docker build -f "docker/Dockerfile.dbmigration" -t km-test-task-db-migration:latest docker || return 1

}

function init_arguments() {
  CLIENT_JAR_PATH="./km-test-task-client/build/libs/km-test-task-client-all.jar"
  SERVER_JAR_PATH="./km-test-task-server/build/libs/km-test-task-server-all.jar"
  LIQUIBASE_CHANGELOGS_DIR="./db-migration/liquibase"
  DOCKER_BUILD_WORKDIR="./docker/workdir"

  rm -rf "$DOCKER_BUILD_WORKDIR"
  mkdir -p "$DOCKER_BUILD_WORKDIR"
}

function main() {

  echo "Starting docker build"

  init_arguments
  build_gradle || die "Error while building jar artifacts"
  build_docker_client_app || die "Error while building client application docker image"
  build_docker_server_app || die "Error while building client application docker image"
  build_docker_db_migration || die "Error while building db migrations docker image"

}

(main "$@")