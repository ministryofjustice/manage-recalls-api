# Pact 

We use [Pact](https://docs.pact.io/) contract testing to verify the APIs we provide and consume are in sync with each other.

To view current pacts you can use the [HMPPS Pact Broker](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)

Consumers of this API should publish a pact file to the broker, the broker then triggers (via a webhook) a build in CircleCI of this project to verify the published pact file works against this version of the API.  

## Pact broker webhooks

* [Pact broker webhooks documentation](https://docs.pact.io/pact_broker/webhooks/)
* [HMPPS Pact broker](https://github.com/ministryofjustice/hmpps-pact-broker)

The webhooks can be setup using the Pact broker UI, or by running the `pact-webhooks/create-webhooks.sh` script (copied from https://github.com/ministryofjustice/hmpps-pact-broker/blob/main/seed/create-webhooks.sh).

e.g.
```CIRCLE_TOKEN=XXX GITHUB_ACCESS_TOKEN=XXX PACT_BROKER_USERNAME=XXX PACT_BROKER_PASSWORD=XXX ./createContractContentChanged.sh```

You will need to create a CircleCI access token, and a Github access token with `repo:status` and SSO enabled.  You will also need the Pact broker username and password, these can be retrieved as follows:

* Request to join the github team [pact-broker-maintainers](https://github.com/orgs/ministryofjustice/teams/pact-broker-maintainers/members) so you have access to the kubernetes secret with the authentication details.  
* Head to https://login.cloud-platform.service.justice.gov.uk/ to get a new `~/.kube/config` 
* Retrieve the username and password using:
  `cloud-platform decode-secret -n  pact-broker-prod -s basic-auth`

(install `cloud-platform` from `https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/cloud-platform-cli.html#the-cloud-platform-cli`)

### Viewing the existing broker webhooks 

You can view all webhooks on the broker by going to https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/webhooks

Or by running:

```curl https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/webhooks | jq``` 