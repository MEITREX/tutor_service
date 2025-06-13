# template-microservice
This serves as a template for the microservices

## Package structure

This package structure is based on multiple sources of best practices in Spring Boot, using roughly the "Package by layer" approach.
- *root*
  - *config*
  - *controller*
  - *dapr*
  - *dto*
  - *exception*
  - *persistence*
    - *entity*
    - *repository*
    - *mapper*
  - *service*
  - *util* (optional, if needed)
  - *validation*

Detailed description of the packages:

### Root package

This should be named after the microservice itself. This is the root package for the microservice. It contains the `Application.java` file (or of similar name), which is the entry point for the microservice. Usually, this is the only class in this package.

### Config package
This package should contain any classes that are used to configure the application. This includes [Sprint Boot configuration classes](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Configuration.html) but might also contain anything else related to configuration the microservice.
The classes that are in this package should not be deleted in the actual microservice as they provide useful functionality.

### Controller package

**Location:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/controller**

This package contains the GraphQL controllers (and other types of controllers if needed). The GraphQL controllers are annotated with the `@Controller` annotation. Controllers contain no business logic, but only delegate the requests to the service layer. They handle the "technical stuff" of the request.

In some services, there is also a class called SubscriptionController which handles all dapr event subscriptions.

More information can be found in
the [Controller package](src/main/java/de/unistuttgart/iste/meitrex/tutor_service/controller/package-info.java).

### Dapr package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/dapr

This package should contain all classes that are used to communicate with Dapr, e.g. using pub sub.

### DTO package

**This package will not be located in the src/main/java folder, but in the build/generated folder.**

This package contains the generated DTOs (data transfer objects) from the GraphQL schema. The DTOs are generated when building the project with gradle. 

If not necessary, no other files should be added manually to this package.

#### Why both DTOs and Entities?

The DTOs are used to transfer data between the GraphQL controller and the service layer. The entities are used to persist data in the database. This is done to separate the data transfer from the data persistence. This is a common approach in Spring Boot applications as it can happen that we want to store more data in the database than we want to transfer to the client or vice versa.

### Exception package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/exception

This package is used for exception handling. Note that with GraphQL, the exceptions are not thrown directly, but are wrapped in a `GraphQLException`, which is different that from the usual Spring Boot approach.

More information can be found in
the [Exception package](src/main/java/de/unistuttgart/iste/meitrex/tutor_service/exception/package-info.java).

### Persistence package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/persistence

This package contains all classes that are used to persist data in the database. This includes the entities, the mapping
logic between entities and DTOs, as well as the repositories.

This package handles the calls to the database and defines the database entities. It is structured into three sub-packages:

#### 1. entity
This package contains the database entities.

#### 2. repository
This package contains the interfaces to the database, also known as Data Access Objects (DAOs), used to perform various database operations. Note that these interfaces may sometimes be empty, especially when the default methods provided by the Spring Framework are sufficient for the required operations.

#### 3. mapper
The 'mapper' package is responsible for the mapping logic between the database entities and the data types defined in the GraphQL schema. Specifically, it maps the database entity classes to the corresponding classes generated from the GraphQL schema.

This structure helps organize the database-related components of the project, making it easier to manage and maintain.

More information can be found in
the [Entity package](src/main/java/de/unistuttgart/iste/meitrex/tutor_service/persistence/entity/package-info.java) and
the [Repository package](src/main/java/de/unistuttgart/iste/meitrex/tutor_service/persistence/repository/package-info.java).

### Service package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/service

This package contains all classes that are used to handle the business logic of the microservice. Services are annotated with the `@Service` annotation. Services contain only business logic and delegate the data access to the persistence layer (repositories). 

More information can be found in
the [Service package](src/main/java/de/unistuttgart/iste/meitrex/tutor_service/service/package-info.java).

### Validation package

**Location**:src/main/java/de/unistuttgart/iste/meitrex/{service_name}/validation

This package should contain the *class-level* validation logic, i.e. the validation logic that is not directly related to a specific field, e.g. validation if an end date is after a start date.

Field-level validation logic should not be placed in this package, but in the graphql schema, via directives. 
If these directives are not sufficient, the validation logic can also be placed in this package.

## Getting Started

### Todos

Follow the guide in the wiki: https://github.com/MEITREX/wiki/blob/main/dev-manuals/backend/new-service.md

Addtionally, after cloning the repository, you need to do the following steps:
- [ ] Setup the gradle files correctly. This means
  - [ ] Change the project name in the `settings.gradle` file
  - [ ] Change the sonar project key in the `build.gradle` file (should be MEITREX_repository_name)
  - [ ] Add/Remove dependencies in the `build.gradle` file
- [ ] Rename the package in the `src/main/java` folder to  a more suitable name (should be the service name)
- [ ] Remove the package-info.java files in the `src/main/java` folder, if the file is present (or update with the microservice specific information)
- [ ] Update the application.properties file in the `src/main/resources` folder (check the TODOS in the file)
- [ ] Change the ports and name of the database in the docker-compose.yml (see wiki on how to)
- [ ] Define the GraphQL schema in the `src/main/resources/schema.graphqls` file
<!-- TODO there probably more TODOs -->


After creating a new service you need to do the following:
- [ ] Add the repository to sonarcloud, follow the instructions for extra configuration, unselect automatic analysis and choose github actions, only the first step needs to be completed
- [ ] Add SONAR_TOKEN to the service repository secrets on Github (this requires you to have admin permissions on sonarcloud) 

### Pull new changes from this template

If this template changes and you want to pull the changes to the actual microservice, you can run the following commands:
```bash
git remote add template https://github.com/MEITREX/template_microservice # only necessary once
git fetch --all
git checkout [branch] # replace [branch] with the branch name you want the changes to be merged into (preferably not main)
git merge template/main --allow-unrelated-histories
# you will probably need to commit afterwars
```

### Guides
The following guides illustrate how to use some features concretely:

* [Building a GraphQL service](https://spring.io/guides/gs/graphql-server/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Validation with GraphQL directives](https://github.com/graphql-java/graphql-java-extended-validation/blob/master/README.md)
* [Error handling](https://www.baeldung.com/spring-graphql-error-handling)

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.6/gradle-plugin/reference/html/)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/3.0.6/reference/htmlsingle/#appendix.configuration-metadata.annotation-processor)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/3.0.6/reference/htmlsingle/#using.devtools)
* [Spring for GraphQL](https://docs.spring.io/spring-boot/docs/3.0.6/reference/html/web.html#web.graphql)
* [Spring Data JPA](https://docs.spring.io/spring-boot/docs/3.0.6/reference/htmlsingle/#data.sql.jpa-and-spring-data)
* [Validation](https://docs.spring.io/spring-boot/docs/3.0.6/reference/htmlsingle/#io.validation)
* [Generating Sonarqube Token](https://docs.sonarqube.org/latest/user-guide/user-account/generating-and-using-tokens/)
* [Adding secrets on Github](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
