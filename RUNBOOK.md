# manage-recalls-api Run Book

## Service or system overview

### Business overview

This provides an API interface for creating and managing recalls within HMPPS.

### Technical overview

Internal API based on Springboot, using Kotlin as the main language. Deployed in kubernetes using the configuration found in [helm_deploy](helm_deploy).

### Service Level Agreements (SLAs)

Office hours (Mon-Fri, 09:00-17:00), best efforts.

### Service owner

The `ppud-replacement` team develops and runs this service.

Contact the [#ppud-replacement](https://mojdt.slack.com/archives/C020S8C0K9U) and [#ppud-replacement-devs](https://mojdt.slack.com/archives/C0223AGGQU8) channels on slack.

### Contributing applications, daemons, services, middleware

- Springboot application based on [hmpps-template-kotlin](https://github.com/ministryofjustice/hmpps-template-kotlin).
- PostgreSQL database for persistence - configured via [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments).
- AWS S3 for document storage - again, configured via [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments).
- [gotenberg](https://gotenberg.dev/) for PDF rendering - included via a helm sub-chart.
- [ClamAV](https://www.clamav.net/) for AV scanning of uploaded files - included via a helm chart.
- [CircleCI](https://circleci.com/) for CI/CD.

## System characteristics

### Hours of operation

_TBC_

### Infrastructure design

The application runs on the [HMPPS Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/) within the `ppud-replacement-<tier>` namespaces (where `tier` is `dev`, `preprod` or `prod`).

The main application runs as a deployment named `manage-recalls-api`, with the following support deployments (all deployed as part of the same helm/kubernetes configuration):

- `manage-recalls-api-gotenberg` - for PDF rendering.
- `manage-recalls-api-clamav` - for AV scanning.

The API is made avialable externally from the cluster via an Ingress.

See the `values-<tier>.yaml` files in the [helm_deploy](helm_deploy) directory for the current configuration of each tier.

### Throttling and partial shutdown

If there is an issue with the service where it is causing load on downstream services and it needs to be shutdown quickly the following command will reduce the number of pod replicas to zero:

```
kubectl -n ppud-development-<tier> scale deployment manage-recalls-api --replicas=0
```

We do not currently have a strategy in place to throttle requests.

### Environmental differences

Infrastructure wise, all three tiers are identical, but `prod` has the following differences:

- It will have more pod replicas of the main application deployment.
- As this is live data, you **must** be SC cleared to work on it or access the data held within it.

### Tools

- [port-forward-db.sh](scripts/port-forward-db.sh) - allows you to connect to the PostreSQL database from your local machine (as external access is blocked).
- [port-forward-http-proxy.sh](scripts/port-forward-http-proxy.sh) - allows you to connect to the S3 bucket from your local machine via the aws-cli (as external access is blocked).

## Security and access control

In order to gain access to the `ppud-replacement-<tier>` namespaces in kubernetes you will need to be a member of the [ministryofjustice](https://github.com/orgs/ministryofjustice) github organisation and a member of the [ppud-replacement-devs](https://github.com/orgs/ministryofjustice/teams/ppud-replacement-devs) team. Once joined, you should have access to the cluster within 24 hours.

You will need to follow the [Cloud Platform User Guide](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#how-to-use-kubectl-to-connect-to-the-cluster) to setup your access from there.

## System configuration

### Configuration management

- Infrastructure is configured via [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments).
- Application configuration is via [helm_deploy](helm_deploy).

### Secrets management

Secrets are stored within the `ppud-replacement-<tier>` namespaces in kubernetes.

Secrets with information from [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments) will be managed via the terraform code in there.

All other secrets (i.e. `manage-recalls-api`) are currently created manually - this will be addressed in [PUD-22](https://dsdmoj.atlassian.net/browse/PUD-22).

## System backup and restore

This is handled by the HMPPS Cloud Platform Team.

## Monitoring and alerting

### Log aggregation solution

Please see [Confluence](https://dsdmoj.atlassian.net/wiki/spaces/PUD/pages/3622830168/Monitoring+Operability#Logging) for more details.

### Log message format

Currently the ELK solution cannot correctly process/transform structured/JSON logging, so a `log4j` single-line output is currently preferred.

### Events and error messages

Please see [Confluence](https://dsdmoj.atlassian.net/wiki/spaces/PUD/pages/3622830168/Monitoring+Operability#Runtime-Error-Reporting) for more details.

### Metrics

Please see [Confluence](https://dsdmoj.atlassian.net/wiki/spaces/PUD/pages/3622830168/Monitoring+Operability#Metrics) for more details.

### Health checks

> How is the health of dependencies (components and systems) assessed? How does the system report its own health?

#### Health of dependencies

> (e.g. Use `/health` HTTP endpoint for internal components that expose it. Other systems and external endpoints: typically HTTP 200 but some synthetic checks for some services)

_TBC_

#### Health of service

> (e.g. Provide `/health` HTTP endpoint: 200 --> basic health, 500 --> bad configuration + `/health/deps` for checking dependencies)

_TBC_

## Operational tasks

### Deployment

We use CircleCI to manage deployments:

- Built docker images are pushed to [quay.io](https://quay.io/repository/hmpps/manage-recalls-api).
- Deployment to kubernetes uses helm.

### Troubleshooting

Please see [Confluence](https://dsdmoj.atlassian.net/wiki/spaces/PUD/pages/3622830168/Monitoring+Operability#Debugging-an-Application-That-Fails-to-Start) for some generic troubleshooting notes.

## Maintenance tasks

#### Identified vulnerabilities

We scan the currently deployed docker containers daily with [trivy](https://github.com/aquasecurity/trivy). If any `HIGH` or `CRITICAL` vulnerabilities are identified the team is notified in the [#ppud-replacement-devs](https://mojdt.slack.com/archives/C0223AGGQU8) slack channel.

### Data cleardown

For the `dev` and `preprod` tiers we have a [flush-env.sh](scripts/flush-env.sh) script that will clear all recalls and associated documents (from both PostgreSQL and S3). **This should only be used when absolutely necessary.**
