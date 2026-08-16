FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN curl -L -o mysql-connector-j.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar

COPY . .

RUN mkdir -p out
RUN javac -cp mysql-connector-j.jar -d out $(find src/main/java -name "*.java")
RUN cp -r src/main/resources/* out/ 2>/dev/null || true

CMD ["java", "-cp", "out:mysql-connector-j.jar", "com.library.ui.ConsoleApp"]