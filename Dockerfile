FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/install/thedome/ /app/
EXPOSE 8080
ENTRYPOINT ["/app/bin/thedome"]
