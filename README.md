# Manage Recall API

[![CircleCI](https://circleci.com/gh/ministryofjustice/manage-recalls-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/manage-recalls-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://manage-recalls-api-dev.hmpps.service.justice.gov.uk/swagger-ui/)

API service for managing recalls


### Code style & formatting
```bash
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```
will apply ktlint styles to intellij and also add a pre-commit hook to format all changed kotlin files.

### Run locally on the command line
Requires postgres:
```docker compose up postgres -d```

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### Run in docker-compose
```bash
docker-compose pull && docker-compose up -d
```

The service should start up using the dev profile running on port 8080

### Run document generation test with gotenberg

In order to run tests against gotenberg running in docker:

```bash
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh
```

The following script runs the document generation tests in its own docker container, this is used by circleCI (this is
very slow running locally, the base docker image needs enhancing so we don't download gradle and all the dependencies
every time)

`scripts/run-docker-integration-tests.sh`

### Full local build

Builds everything, runs the unit tests, integration tests, start gotenberg and document generation tests

`./build.sh`

# Swagger UI

We are using springfox to autogenerate the swagger docs. Go to http://localhost:9091/swagger-ui/ locally to see what it
generates. See `SpringFoxConfig` to control which endpoints etc it hits, currently it is generating for *everything*

### Dependee APIs
This service depends on a set of service APIs supplied within MoJ.  
Links to Swagger for them can be found via e.g. https://structurizr.com/share/56937/documentation/*#Published%20APIs

An example is at: https://prisoner-offender-search-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/prisoner-search-resource/findByCriteria_1

Those APIs can be tried out from a Dev laptop using e.g. Postman, curl etc. 
The script `./scripts/getAccessToken.sh <env>` can be used to output an authorisation Bearer Token 
to give access to those end-points, i.e. using `--header 'Authorization: Bearer <token>'` with curl. 
Use lowercase env, defaults to `dev` with no argument.