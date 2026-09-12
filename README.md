# KM test task

# 1. Prerequisites
- JDK 25
- docker with docker compose plugin
- Linux / WSL for running build scripts

# 2. Project structure
- `db-migration`
  - Liquibase xml migration scripts. Create database schema creation and initial data
- `docker`
  - app images Dockerfiles, docker compose file and workdir for dependencies
- `km-test-task-client`
  - Kotlin CLI application for running concurrent test scenarios.
  - Performs concurrent REST API calls to server app
- `km-test-task-server`
  - backend REST API service
- `scripts`
  - bash scripts for building and running application

# 3. Docker containers description

1. `app-db` - postgresql database 
    - `Dockerfile.dbmigration`
2. `db-migration` - runs liquibase migration scripts
   - depends on `app-db`
3. `app-server` - REST API server for dictionary domain
   - `Dockerfile.server`
4. `app-client` - interactive CLI application for running concurrency test scenarios
   - `Dockerfile.client`


```
*** docker-compose **************************************
*                                                       *
*   ----------------                                    *
*   | db-migration |                                    *
*   ----------------                                    *
*      (1) |                                            *
*          V                                            *
*      ----------                                       *
*      | app-db |                                       *
*      ----------                                       *
*          ^                                            *
*      (2) |  (r2dbc:postgres://app-db:5432/postgres)   *    
*          |                                            *
*   --------------                                      *           -----------
*   | app-server |  <----------(http://localhost:44555/openapi)-----| browser |
*   --------------                                      *           -----------
*          ^                                            *
*      (3) |  (http://app-server:8080/)                 *
*          |                                            *
*   --------------                                      *           ------------------------------
*   | app-client |  <---------------------------------------------- | docker exec -it app-client |
*   --------------                                      *           ------------------------------
*                                                       *
*********************************************************
```
-----

# 4. Spin up docker compose test environment

1. Make scripts executable
```shell
chmod +x ./scripts/*
```

2. Build docker images
```shell
./scripts/docker_build.sh
```

3. Run docker compose

```shell
./scripts/compose_up.sh
```

4. Stop when finished

```shell
./scripts/compose_down.sh
```

# 5. Use and test application

## 5.1 Swagger UI

- http://localhost:44555/openapi

## 5.2 CLI for concurrency tests

```shell
./scripts/client-cli-connect.sh
```

Contains set of testing scenarios to launch.

CLI runs as container inside docker compose stack.

Makes requests over internal docker compose network to `app-server` host http://app-server:8080

Scripts runs `docker attach` or `docker start` based `app-client` container state.


#### 5.2.1 CLI overview
Application runs infinitely until `quit` command is entered.

Type command number (e.g. `3`) in console to execute.
- once command completed menu will be opened again

```shell
=== Available Commands ===
1    - Print all counters
2    - Concurrent Increment scenario - correct concurrency control
3    - Concurrent Increment scenario - unsafe update uperation
4    - Concurrent Insert
help - Display this help menu (or 'h')
quit - Exit the application (or 'q')
==========================
Enter command > 
```
