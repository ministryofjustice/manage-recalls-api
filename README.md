# Manage Recall API

- [![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=for-the-badge&logo=github&label=MoJ%20Compliant&query=%24.data%5B%3F%28%40.name%20%3D%3D%20%22manage-recalls-api%22%29%5D.status&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fgithub_repositories)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/github_repositories#manage-recalls-api "Link to report")
- CI Status: [![CircleCI](https://circleci.com/gh/ministryofjustice/manage-recalls-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/manage-recalls-api)
- PACT Status: [![PACT](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/pacticipants/manage-recalls-api/latest-version/main/can-i-deploy/to/dev/badge)](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)
  [![PACT](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/pacticipants/manage-recalls-api/latest-version/main/can-i-deploy/to/preprod/badge)](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)
  [![PACT](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/pacticipants/manage-recalls-api/latest-version/main/can-i-deploy/to/prod/badge)](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)
- API Docs: [![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://manage-recalls-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

UK (ex-Scotland) MoJ API service for managing recalls. The main Slack channel for
the maintaining team is currently: `#ppud-replacement`

## Usage

A Recall in this sense is for an offender out on license to be recalled into detention
based on infringement of license conditions. Multiple agencies
participate in the Recall process including MoJ, HMPPS, prisons and police.

The primary software client for this service is the related UI `manage-recalls-ui`.

### Limitations

Current limitations of this service include:

- it is not yet the reference source for any operational Recall information:
  the PPUD system remains that.
- no historical data, i.e. from PPUD, is currently accessible via this service (or from any other DPS/HMPPS
  digital service).
- the exposed API is WIP: it has been created to satisfy the specific UI client; modifications to it may be
  appropriate for more general consumption.

### See Also

- pact/README.md
- helm_deploy/README.md

### Code style & formatting

```bash
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```

will apply ktlint styles to intellij and also add a pre-commit hook to format all changed kotlin files.

### Run the service locally on the command line

Requires the following:

```
docker compose up -d postgres minio fake-prisoner-offender-search-api fake-prison-api fake-prison-register-api fake-court-register-api gotenberg clamav
```

Start with dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

You can now check the API is up and running at: http://localhost:8080/health/liveness

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

Note: this script will start up a `postgresql` and `minio` instance using `docker-compose` in the background.

### Swagger UI

We are using `springdoc` to generate swagger docs. When local running the Swagger html can be found at:
http://localhost:8080/swagger-ui/index.html and the OpenApi v3 API json at http://localhost:8080/v3/api-docs.
See class `OpenApiConfiguration` for any customisations, e.g. the
un-wrapping of Kotlin data classes as response types. Note also when making changes that this
swagger output is consumed by our UI project to define typescript types
as per: https://github.com/ministryofjustice/manage-recalls-ui/blob/main/docs/tests.md#typescript-definitions-generated-from-manage-recalls-api-swagger-endpoint.

### Dependee APIs

This service depends on a set of service APIs supplied within MoJ.
Links to Swagger for them can be found via e.g. https://structurizr.com/share/56937/documentation/*#Published%20APIs

An example is at: https://prisoner-offender-search-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/prisoner-search-resource/findByCriteria_1

Those APIs can be tried out from a Dev laptop using e.g. Postman, curl etc.
The script `./scripts/getAccessToken.sh <env>` can be used to output an authorisation Bearer Token
to give access to those end-points, i.e. using `--header 'Authorization: Bearer <token>'` with curl.
Use lowercase env, defaults to `dev` with no argument.

### Postman against locally running API

Create a bearer token from local hmpps-auth (whilst we still use it) using:
`curl -sX POST "http://localhost:9090/auth/oauth/token?grant_type=password&username=PPUD_USER&password=password123456" -H 'Authorization: Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==' | jq .access_token`

## Static inputs

Various non-code content of the project has been generated by one-off processing.

### Document templates

Section TBD.

## AUDIT

see [here](AUDIT.md)
