# KM test task

# Table of Contents
- [1. Prerequisites](#1-prerequisites)
- [2. Project structure](#2-project-structure)
- [3. Docker container description](#3-docker-containers-description)
- [4. Spin up docker compose test environment](#4-spin-up-docker-compose-test-environment)
- [5. Use and test application](#5-use-and-test-application)
  - [5.1 Swagger UI](#51-swagger-ui)
  - [5.2 CLI for concurrency tests](#52-cli-for-concurrency-tests)
    - [5.2.1 CLI overview](#521-cli-overview)
    - [5.2.2 CLI commands overview](#522-cli-commands-overview)
- [6. API design](#6-api-design)
- [7. Concurrent increment correctness](#7-concurrent-increment-correctness)

# 1. Prerequisites
- JDK 25
- docker with docker compose plugin
- Linux / WSL for running build scripts

# 2. Project structure
- `db-migration`
  - Liquibase xml migration scripts. Create database schema creation and initial data
- `docker`
  - app images Dockerfiles, docker compose file and workdir for dependencies
- `docs`
  - OpenAPI specification yaml
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

```
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
#### 5.2.2 CLI commands overview

##### Print all counters

Invokes get all API and prints all counters to console

##### Concurrent Increment scenario - correct concurrency control

1. Creates new counter with UUID as name and random initial value (from 100 to 1000)
2. Spawns 300 concurrent coroutines. Coroutines wait for signal on `CompletableDeferred` object
3. Main thread completes Completable
4. 300 coroutines send increment API call
5. Main thread repeats steps 1-4 100 times
6. Main thread prints report with actual / expected counter value
```
Finished counter increment:
Counter name:                c69eff3a-d0ff-4b88-82eb-75db3850a80a
Initial value:               496
expected increment:          30000
expected value:              30496
actual value:                30496
total increment operations:  30000
unique response counters:    30000
```

##### Concurrent Increment scenario - unsafe update operation

Same as command `2`, but API without concurrency control is used. Lost updates possible
```
Finished counter increment:
Counter name:                bde42eeb-7a0c-49ad-8872-c0719b0f001b
Initial value:               689
expected increment:          30000
expected value:              30689
actual value:                987
total increment operations:  30000
unique response counters:    30000
```

##### Concurrent Insert
1. Generates UUID for counter name
2. Spawns 300 concurrent coroutines. Coroutines wait for signal on `CompletableDeferred` object
3. Main thread completes Completable
4. 300 coroutines send create API call for the generated counter name. Each coroutine generates random initial value
5. Coroutine checks API response status and resolves Deferred object with success / failed boolean flag and initial counter value
6. Main thread await all coroutines
7. Main thread prints statics
   - number of success / failed coroutines
   - success counter value
   - actual counter value

```
Finished concurrent creation of items:
Counter name:         5d4b497d-e507-405a-b8d2-6dc9fcb73559
total requests:       300
successful requests:  1
failed requests:      299
actual counter:       616
expected counter:     616
```


# 6. API design

#### General idea
- `/api/v1` - versioning
- `/api/v1/dictionaries/default` - dictionaries resource with one "default" dictionary for now
- `/api/v1/dictionaries/default/counters/{name}` - counters resource within the dictionary
- `/api/v1/dictionaries/default/counters/{name}/increments` - ephemeral resource, collection of "increment" events for the counter

#### Endpoints description
Basic resource get / delete operations
- `GET /api/v1/dictionaries/default/counters`
- `GET /api/v1/dictionaries/default/counters/{name}`
- `DELETE /api/v1/dictionaries/default/counters/{name}`

Create resource with full resource URI - PUT.
Operation is idempotent - re-try does not cause creation of new resource.
- `PUT /api/v1/dictionaries/default/counters/{name}`


Add "increment" event to collection without specifying full "increment" resource ID - POST. 
Operation is not idempotent - consecutive re-tries increment counter more and more
- `POST /api/v1/dictionaries/default/counters/{name}/increments`
- `POST /api/v1/dictionaries/default/counters/{name}/increments-unsafe`

# 7. Concurrent increment correctness

Increment operation runs as atomic sql update operation at READ COMMITED transaction isolation level.

PostgreSQL guarantees `value = value + 1` will be atomically run on newest version of row on READ COMMITED isolation level.
```sql
UPDATE counters
SET value = value + 1
WHERE name = '{name}'
RETURNING name, value
```

