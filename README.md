# RemoteBank Transactions Project

## Overview

This project is designed to process and analyze banking transactions in real-time using Apache Flink and Kafka. The processed data is stored in a Redis database for quick access and further analysis.

## Prerequisites

Before you begin, ensure you have the following installed on your local machine:

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)
- [Maven](https://maven.apache.org/install.html)
- [Java JDK 8 or later](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

## Installation

Follow these steps to set up and run the project:

1. **Clone the project repository:**

    ```sh
    git clone https://github.com/yourusername/remotebank-transactions.git
    cd remotebank-transactions
    ```

2. **Build the project using Maven:**

    ```sh
    mvn clean install
    ```

3. **Start the Docker containers:**

    ```sh
    docker-compose up -d
    ```

   This command will start the following services:
    - Zookeeper
    - Kafka
    - Flink Job Manager
    - Flink Task Manager
    - Redis
    - Transactions Producer
    - Transactions Backup Job
    - Transactions ML Features Job

4. **Verify the setup:**

    - Open the Flink UI in your browser: [http://localhost:8081](http://localhost:8081)
    - Check the running jobs to ensure the Flink jobs are started and running.