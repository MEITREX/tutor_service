# AI Tutor Service
This AI Tutor Service in MEITREX serves as a connection to the LLM for the AI Chat Bot.
The srvice is structured in packages.

Detailed description of the packages:

### Config package
This package should contain any classes that are used to configure the application.
This includes [Sprint Boot configuration classes](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Configuration.html) but might also contain anything else related to configuration the microservice.
The classes that are in this package should not be deleted in the actual microservice as they provide useful functionality.

### Controller package

**Location:src/main/java/de/unistuttgart/iste/meitrex/tutor_service/controller**

This package contains the GraphQL controllers (and other types of controllers if needed).
The GraphQL controllers are annotated with the `@Controller` annotation.
Controllers contain no business logic, but only delegate the requests to the service layer.
They handle the "technical stuff" of the request.

In some services, there is also a class called SubscriptionController which handles all dapr event subscriptions.


### Exception package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/tutor_service/exception

This package is used for exception handling.
Note that with GraphQL, the exceptions are not thrown directly, but are wrapped in a `GraphQLException`, which is different that from the usual Spring Boot approach.


### Persistence package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/tutor_service/persistence

This package contains all classes that are used to persist data in the database. This includes the entities, the mapping
logic between entities and DTOs, as well as the repositories.

This package handles the calls to the database and defines the database entities. It is structured into three sub-packages:

#### 1. entity
This package contains the database entities.

#### 2. repository
This package contains the interfaces to the database, also known as Data Access Objects (DAOs), used to perform various database operations.
Note that these interfaces may sometimes be empty, especially when the default methods provided by the Spring Framework are sufficient for the required operations.

#### 3. mapper
The 'mapper' package is responsible for the mapping logic between the database entities and the data types defined in the GraphQL schema.
Specifically, it maps the database entity classes to the corresponding classes generated from the GraphQL schema.

This structure helps organize the database-related components of the project, making it easier to manage and maintain.

### Service package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/tutor_service/service

This package contains all classes that are used to handle the business logic of the microservice.
Services are annotated with the `@Service` annotation.
Services contain only business logic and delegate the data access to the persistence layer (repositories).

### Validation package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/tutor_service/validation

This package should contain the *class-level* validation logic, i.e. the validation logic that is not directly related to a specific field, e.g. validation if an end date is after a start date.

Field-level validation logic should not be placed in this package, but in the graphql schema, via directives. 
If these directives are not sufficient, the validation logic can also be placed in this package.

## Getting started
A guide how to start development can be
found in the [wiki](https://meitrex.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).

After creating a new service you need to do the following:
- [ ] Add the repository to sonarcloud, follow the instructions for extra configuration, unselect automatic analysis and choose github actions, only the first step needs to be completed
- [ ] Add SONAR_TOKEN to the service repository secrets on Github (this requires you to have admin permissions on sonarcloud)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a GraphQL service](https://spring.io/guides/gs/graphql-server/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Validation with GraphQL directives](https://github.com/graphql-java/graphql-java-extended-validation/blob/master/README.md)
* [Error handling](https://www.baeldung.com/spring-graphql-error-handling)
