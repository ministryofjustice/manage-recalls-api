# Pact Contract Testing

We use [Pact](https://docs.pact.io/) contract testing to verify the APIs we provide and consume are in sync with each other.

- [Pact Documentation](https://pact.io/)
- [HMPPS Pact Broker](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)

The `manage-recalls-api` verifies contracts defined by it's consumers. If a consumer publishes a new contract to the Pact Broker, it will trigger a CI build in `manage-recalls-api` to verify it can still satisfy that contract.

## Pact provider tests

The pact provider tests can be found in `src/test/kotlin/uk/gov/justice/digital/hmpps/managerecallsapi/integration/pact/provider` and are of the format `*PactTest`, they are run as part of the CircleCI `pact` workflow in the `verify_pact_and_publish` job.

When running locally the Pact tests will verify the latest published contracts with the `main` tag. You can override this by specifying a different tag or pact
file in the individual test, as below.

## Verification/maintenance locally

See comments in e.g. `ManageRecallsUIPactTest` to configure the source for
the Consumer contract specification to verify, i.e. annotations like one of:

```
@PactFolder("../manage-recalls-ui/pact/pacts")
@PactBroker(
  consumerVersionSelectors = [ VersionSelector(tag = "pact") ]
)
```

etc.

As per local running, executing the PACTs here also requires e.g. postgres and minio:

`docker compose up -d postgres minio`

Then API PACT verification alone can be executed with:

```
./gradlew verifyPactAndPublish
```

## Pact broker webhooks

- [Pact broker webhooks documentation](https://docs.pact.io/pact_broker/webhooks/)

The webhooks are set up as part of the [hmpps-pact-broker](https://github.com/ministryofjustice/hmpps-pact-broker) codebase - the `apply_webhooks` "action" to be precise. We currently control two webhooks:

- [manage-recalls-api](https://github.com/ministryofjustice/hmpps-pact-broker/blob/main/seed/webhook-manage-recalls-api.json) - provider webhook - This is called whenever a consumer contract is changed. It runs a pipeline on CircleCI to verify it can still satisfy the contract.
- [manage-recalls-ui-feedback](https://github.com/ministryofjustice/hmpps-pact-broker/blob/main/seed/webhook-manage-recalls-ui-feedback.json) - consumer webhook - This is called whenever a new contract is published, or a contract has been verified. It updates the github commit "status" for the "main" branch of the consumer (`manage-recalls-ui`) to say if the contract is verified or not.

In order for these webhooks to work successfully, all repos **must** have the [hmpps-developers team](https://github.com/orgs/ministryofjustice/teams/hmpps-developers) added with "**Write**" access.

### Gaining access to the Pact Broker credentials

If you need to gain access to the Pact Broker credentials (to manually interact with the API), perform the following steps.

- Request to join the github team [pact-broker-maintainers](https://github.com/orgs/ministryofjustice/teams/pact-broker-maintainers/members) so you have access to the kubernetes secret with the authentication details.
- Head to https://login.cloud-platform.service.justice.gov.uk/ to get a new `~/.kube/config`
- Retrieve the username and password using: `cloud-platform decode-secret -n pact-broker-prod -s basic-auth`

(install `cloud-platform` from `https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/cloud-platform-cli.html#the-cloud-platform-cli`)

### Viewing the existing broker webhooks

You can view all webhooks on the broker by going to https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/webhooks

Or by running:

```sh
curl https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/webhooks | jq
```
