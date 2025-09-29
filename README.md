# HMCTS Dev Test Backend
Previous context:
```
This will be the backend for the brand new HMCTS case management system. As a potential candidate we are leaving
this in your hands. Please refer to the brief for the complete list of tasks! Complete as much as you can and be
as creative as you want.

You should be able to run `./gradlew build` to start with to ensure it builds successfully. Then from that you
can run the service in IntelliJ (or your IDE of choice) or however you normally would.

There is an example endpoint provided to retrieve an example of a case. You are free to add/remove fields as you
wish.
```

## Approach

### Basic Principles

I approached this challenge initially on the basis of producing a minimum viable
product to complete the brief. I made the mistake of assuming this repository's Case
entity was the only entity needed, having misread the brief on the management of
tasks as cases. I therefore started by adding Spring JPA configuration for a repository
to cover the CRUD requirements from the brief.

Additionally, I added the necessary dependencies to `build.gradle` for testing with an
H2 database as managing the persistence semantics and ddl configuration would merely
slow the process of development.

I modified the existing class to use a `UUID` for ID fields as this is my default choice
for various reasons including preventing sequentially incremented numerical IDs
leaking information. While this isn't key for this project, I prefer to use them in all
projects if possible.

#### Repository design and key features

The default JPA repository classes provide all the key requirements for this brief
through the `CrudRepository` interface. However, I extended the features supported to
include paginated searching. While the functions for this are somewhat inelegant due
to requiring multiple arguments, and needing a fully formed UUID to search by ID.

I also added a constraint to require that the case number be unique as that seems to
fit with the publicly available HMCTS interface requirements (e.g.
[https://casetracker.justice.gov.uk/](https://casetracker.justice.gov.uk/)).

#### Controller Design

The controllers were initially designed directly interacting with the repositories,
calling the functions directly from within the request handling function. I chose to
provide the basic CRUD features through the typical HTTP `GET` `POST` and `DELETE`
requests as creating and updating are both effectively `POST` operations.

As development progressed alongside the frontend, I opted to add search functionality
for the cases and find-by-case for the tasks. These were to cater to the basic
requirements of a user to find the case and browse it's attached tasks in pages.

#### Testing Development

My test implementations focus on testing complete routes with Smoke and Integration tests
for both user-experience simulation and constraint/relationship verification.

