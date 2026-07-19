# Etapa 1: Build da aplicação
FROM maven:3.9.16-eclipse-temurin-25 AS builder

WORKDIR /app


# Copia apenas o pom.xml primeiro para aproveitar cache do Docker
COPY pom.xml ./

# Download das dependências
RUN mvn dependency:go-offline -B

# Copia o código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests

# Etapa 2: Imagem final
FROM eclipse-temurin:25-jre-jammy

# Usa usuário não-root para segurança
RUN addgroup --system --gid 1000 appuser && \
    adduser --system --uid 1000 --gid 1000 appuser

WORKDIR /app

# Copia o JAR
COPY --from=builder --chown=appuser:appuser /app/target/*.jar app.jar

# Cria diretórios temporários
RUN mkdir -p /app/temp/cert/bb_upload /app/temp/google /app/temp/notifications && \
    chown -R appuser:appuser /app/temp && \
    chmod -R 755 /app/temp

# Variáveis de ambiente para Railway
# javax.net.debug=ssl:handshake foi testado para diagnostico do mTLS (BB/Sicoob) e revertido:
# o dump do truststore default a cada SSLContext (R2, Google Drive, SendGrid, Focus NFe, etc.)
# excede o rate limit de log do Railway (500/s) e derruba as linhas que importam.
# Ver diagnostico-mtls.md - o diagnostico passou a usar o LoggingX509KeyManager (1 linha/handshake).
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV PORT=8080

# Expõe porta (Railway usa variável PORT)
EXPOSE 8080

USER appuser

# Comando de inicialização compatível com Railway
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
