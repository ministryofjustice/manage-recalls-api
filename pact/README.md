# Pact Contract Testing

We are using Pact for contract testing our services.  
[Pact Documentation](https://pact.io/)

[HMPPS Pact Broker](https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk/)

The `manage-recalls-api` verifies contracts defined by it's consumers.  If a consumer publishes a new contract to the Pact 
Broker, it will trigger a CI build in `manage-recalls-api` to verify it can still satisfy that contract.  

The pact provider tests can be found in `src/test/kotlin/uk/gov/justice/digital/hmpps/managerecallsapi/integration/pact/provider` 
and are of the format `*PactTest`, they are run as part of the CircleCI `pact` workflow in the `verify_pact_and_publish` job.

When running locally the Pact tests will verify the latest published contracts with 
the `main` tag.  You can override this by specifying a different tag or pact 
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

Then API PACT verification alone can be executed with: 
```
./gradlew verifyPactAndPublish
```
