# Financial Transaction Processing System

This project is a microservices-based system designed to process financial transactions, handle data corrections, and maintain a shadow ledger. It uses Kafka for asynchronous communication between services and PostgreSQL for data persistence.

## Architecture

The system consists of several microservices that communicate via a Kafka message broker.

-   **`drift-correction-service`**: This service is responsible for identifying and correcting data drifts. It connects to a PostgreSQL database and publishes correction events to the `transactions.corrections` Kafka topic.
-   **`shadow-ledger-service`**: This service consumes raw transactions and corrections from the `transactions.raw` and `transactions.corrections` Kafka topics to maintain an up-to-date shadow ledger.

## Technologies Used

-   **Framework**: Spring Boot
-   **Language**: Java
-   **Build Tool**: Maven
-   **Messaging**: Apache Kafka
-   **Database**: PostgreSQL
-   **Containerization**: Docker & Docker Compose

## Prerequisites

Before you begin, ensure you have the following installed on your system:
-   Java (Version 17 or higher)
-   Apache Maven
-   Docker and Docker Compose

## Getting Started

You can run this project using Docker Compose (recommended) or by running each service locally.

### Running with Docker (Recommended)

This is the simplest way to get the entire system running, including all services and backing infrastructure (Kafka, PostgreSQL).

1.  **Build the applications:**
    From the root directory, run the Maven command to build the JAR files for each service.
    ```sh
    mvn clean install
    ```

2.  **Start the services:**
    Use Docker Compose to build the images and start all the containers.
    ```sh
    docker-compose up --build
    ```
    The services will now be running and can communicate with each other using their service names (e.g., `kafka`, `postgres`).

### Running Locally

To run the services directly from your IDE or command line, you must ensure that Kafka and PostgreSQL are already running and accessible from your local machine.

1.  **Start Infrastructure:**
    You can use the provided `docker-compose.yml` to start only the backing services.
    ```sh
    docker-compose up -d kafka postgres
    ```

2.  **Update Configuration:**
    For each service (`shadow-ledger-service`, `drift-correction-service`), you must update the `src/main/resources/application.yml` file to point to `localhost` instead of the Docker service names.

    **Example for `drift-correction-service`:**
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/postgres
      kafka:
        bootstrap-servers: localhost:9092
    ```
    **Example for `shadow-ledger-service`:**
    ```yaml
    spring:
      kafka:
        consumer:
          bootstrap-servers: localhost:29092 # Ensure this port matches your Kafka setup
        producer:
          bootstrap-servers: localhost:29092
    ```

3.  **Run the Applications:**
    Start each Spring Boot application from your IDE or using the command line:
    ```sh
    mvn spring-boot:run
    ```

## Configuration

Application settings are managed in the `src/main/resources/application.yml` file within each service's directory. Key properties include:

-   `server.port`: The port the service will run on.
-   `spring.datasource.url`: The JDBC connection string for the PostgreSQL database.
-   `spring.kafka.bootstrap-servers`: The address of the Kafka broker.

Remember to use service names (`postgres`, `kafka`) for hostnames when running in Docker and `localhost` when running locally.
# Massai_final
# Massai_final
# Massaifinal
# Massaifinal
