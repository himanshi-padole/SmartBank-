
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY MAIN.java ApiServer.java ./

RUN javac MAIN.java ApiServer.java

EXPOSE 8080

CMD ["java", "ApiServer"]

