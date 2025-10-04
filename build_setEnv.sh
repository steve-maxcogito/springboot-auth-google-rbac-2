#!/bin/bash

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/maxcogito_auth
export SPRING_DATASOURCE_USERNAME=maxcogito
export SPRING_DATASOURCE_PASSWORD=Spartan!77
export APP_JWT_SECRET='really-long-random-string'

mvn clean spring-boot:run
# or
java -jar target/your-app.jar
