# Soft Drinks Industry Levy

[ ![Download](https://api.bintray.com/packages/hmrc/releases/soft-drinks-industry-levy/images/download.svg) ](https://bintray.com/hmrc/releases/soft-drinks-industry-levy/_latestVersion)

## About
The Soft Drinks Industry Levy (SDIL) digital service is split into a number of different microservices all serving specific functions which are listed below: 

Liability tool - Standalone frontend service that is used to check a company's liability in regards to the levy.

Frontend - The main frontend for the service which includes the pages for registration.

Backend - The service that the frontend uses to call HOD APIs to retrieve and send information relating to business information and subscribing to the levy.

Stub - Microservice that is used to mimic the DES APIs when running services locally or in the development and staging environments.

This is the backend service, which acts as an adaptor between the registration frontend, and the ETMP APIs. It also handles the enrolment callback from tax-enrolments, and prevents duplicate submissions.

For details about the sugar tax see [the GOV.UK guidance](https://www.gov.uk/guidance/soft-drinks-industry-levy)

## APIs
#### POST       /subscription/:idType/:idNumber/:safeId
Submits a subscription to ETMP. Fails if a subscription has already been submitted for a given safeId.

#### GET        /subscription/:idType/:idNumber
Retrieves a subscription from ETMP if it exists, or returns 404 if no record exists.

#### GET        /check-enrolment-status/:utr
Looks for a subscription in the service's pending buffer and in ETMP.

Returns 200 OK and the ETMP subscription record if it exists, 202 ACCEPTED if only a pending record exists, or 404 NOT_FOUND if no record exists.

#### GET        /rosm-registration/lookup/:utr
Looks up the Business Partner Record in ROSM. Returns 404 NOT_FOUND if no organisation record exists in ROSM.

#### POST       /tax-enrolment-callback/
Handles the callback from tax enrolments when the subscription is activated.

See [here](https://github.com/HMRC/tax-enrolments#put-tax-enrolmentssubscriptionssubscriptionidissuer) and [here](https://github.com/HMRC/tax-enrolments#put-tax-enrolmentssubscriptionssubscriptionidsubscriber) for details

#### GET        /check-direct-debit-status/:sdilRef
Looks for an existing active direct debit in ETMP.

Returns 404 if the SDIL reference is not known.
If the SDIL reference is known then and a has an active SDIL direct debit then a 200 response with a JSON response body is returned like:
```{ "directDebitMandateFound" : true }```  
If the SDIL reference is known then and does not have an active SDIL direct debit then a 200 response with a JSON response body is returned like:
```{ "directDebitMandateFound" : false }```

## Running from source
Clone the repository using SSH:

`git@github.com:hmrc/soft-drinks-industry-levy.git`

If you need to setup SSH, see [the github guide to setting up SSH](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)

Run the code from source using 

`sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes`

The APIs are then accessible at `http://localhost:8701`

## Running through service manager

Run the following command in a terminal: `nano /home/<USER>/.sbt/.credentials`

See the output and ensure it is populated with the following details:

```
realm=Sonatype Nexus Repository Manager
host=NEXUS URL
user=USERNAME
password=PASSWORD
```

*You need to be on the VPN*

Ensure your service manager config is up to date, and run the following command:

`sm --start SDIL_ALL -f`

This will start all the required services

## Running test coverage

To run tests with coverage use:
```
sbt clean coverage test
```

To generate a coverage report: 
```
sbt coverageReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
