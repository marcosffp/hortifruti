# Etapa 1: Build da aplicação
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /app

# Copia apenas o pom.xml primeiro para aproveitar cache do Docker
COPY pom.xml ./

# Download das dependências com cache eficiente
RUN mvn dependency:go-offline -B --fail-never

# Copia o código fonte
COPY src ./src

# Build otimizado com camada separada para dependências
RUN mvn clean package -DskipTests \
    -Dmaven.test.skip=true \
    -Dmaven.compile.fork=true \
    -T 1C

# Etapa 2: Imagem final minimalista
FROM eclipse-temurin:21-jre-jammy

# Usa usuário não-root para segurança
RUN addgroup --system --gid 1000 appuser && \
    adduser --system --uid 1000 --gid 1000 appuser

WORKDIR /app

# Copia o JAR de forma explícita
COPY --from=builder --chown=appuser:appuser /app/target/*.jar app.jar

# Cria diretório temp com permissões
RUN mkdir -p /app/temp/cert /app/temp/google /app/temp/notifications && \
    chown -R appuser:appuser /app/temp

# Otimizações JVM para containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV SPRING_CONFIG_LOCATION="classpath:/application.properties"

# Expõe porta
EXPOSE 8080

# Muda para usuário não-root
USER appuser

# Health check opcional
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entrypoint otimizado
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]