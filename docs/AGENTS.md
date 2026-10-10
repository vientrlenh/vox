# AGENTS.md

## 1. Project Overview 
- Before this file was created, this project is a server-side of a system that support evaluating AI-based oral exams for high school students. Where the normal users have to belong to a specific school in order to use, participate an exam, or practice english oral learning sessions. 


## 2. Project Structure
This repository is using domain-driven design: 
- Domain: Contains pure business models, repositories, validations, no third-party dependency here (including Lombok)
- Application: Business handler layer, where the use cases are placed in (only allow spring framework and lombok import)
- Infrastructure: Contains configurations and third-party dependency configuration, initializer, scheduler
- Interfaces: Contains HTTP handlers and routing for REST API, GraphQL, Apache Kafka external communication, and GRPC

## 3. Setup Information
The project currently using Spring Boot 4.1.1
- Build: `./gradlew build`
- Test: `./gradlew test`
(In the future, the test will be rewritten, allow running unit tests and integration tests independently)

## 4. Conventions: 
### Style
- Variable declaration: The old style is using "var" to declare a variable, but now it is moving to Type declaration
- Request models come from the HTTP contain attributes like LocalDate, Instant, etc. need to be specified directly instead of String like the current implementation
- Comments & messages: Use "English"
- The maximum rows of a methods contains is less than 100 rows

## 5. Rules for Agents: 
- Only using the official APIs of the current version of the library/framework (e.g. Spring Boot 4.1.1 with it bundled libraries), deprecated or older apis/methods are not allowed
- A turn only allowed for editing a method, a file, and must be less than 150 rows, meaning a turn must not edit multiple files, or write a long method
- If it is a complex flow or logic, ask for picking a file to edit and breakdown which other files should be done next
- Only edit which the prompt contains, if it contains any edge cases that are not covered, leave it as a note at the end of the turn
- Use comments for only complex methods or logics, where the code itself can't tell all the works it does
- After editing, if the prompt does not contain any section for running build or test command, do not do that