# Initial Order Management System Setup

## Objective
Create a complete order management system for a streaming service with orders, subscriptions, and a REST API.

## Requirements

### Core Features
Orders can be:
- created
- cancelled
- contains a product ID

Products are:
- monthly subscription
- annual subscription

When an order is created, so is a subscription which lasts for as long as the period the order describes, for example monthly subscription lasts for one calendar month, an annual lasts for one calendar year.
Subscriptions should link to the order they relate to.

### API Requirements
The service should have this API:
- create order
- list orders for a user
- list user subscriptions

## Technical Stack

### Core Technologies
- written in scala 3.3.6
- uses [http4s](https://http4s.org/) and type level libraries
- store data in postgres
- use sbt 1.11.1 for the build
- use [Flyway](/Users/channing/Documents/Companies/Monoidal/Clients/ITV/Code) to manage the database
- use scala [doobie](https://github.com/typelevel/doobie) 1.0.0-RC9 for managing persistence in scala

### Code Style
- Use the projects in the examples directory to inform the style of the project
  - not included in the repo as they aren't projects I could share publicly
- layer the app:
  - the http route should just deal with http requests and responses, calling an internal service API to do the work
  - the internal service API should use model objects and consist of a trait with concrete implementation to facilitate testing with mocks
  - the internal service API should be given a Store API, a trait representing the database store which will facilitate testing with mocks

### Testing Strategy
- everything should be unit tested using [munit](https://scalameta.org/munit/) and [munit-cat-effect](https://typelevel.org/munit-cats-effect/)
- test the storage implementation using [scala testcontainers](https://github.com/testcontainers/testcontainers-scala) and assuming docker is running on the machine

## Git Setup
Initialise a git repo, add all the code except the examples dir, and push to https://github.com/channingwalton/orders.git

Add a sbt alias called `ci`, that runs all the tests.

Add a Github action to build the code and add tests using:

- uses: actions/checkout@v4
- uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: 21
        cache: sbt
    - uses: sbt/setup-sbt@v1
      with:
        sbt-runner-version: 1.11.2
    - name: Build and test
      shell: bash
      run: sbt ci

## Documentation
Write a readme.md that describes the project and its API.

## Process Requirements
- Before pushing code to the repo, run `sbt commitCheck` and only push when that passes

## Maintenance
Run `sup` on the command line to see what can be updated. If after updating all the tests pass then commit and push.

[Aside: sup is my alias for `sbt ";dependencyUpdates; reload plugins; dependencyUpdates"`]

## Status
🟢 **Completed** - Initial setup done, all features implemented and tested.