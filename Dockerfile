###############
# BUILD STAGE #
###############

FROM alpine:3.23 AS build

RUN apk add --no-cache openjdk21 unzip

WORKDIR /kepler

# Copy only build configuration first for better layer caching
COPY gradlew settings.gradle ./
COPY gradle/ gradle/
COPY Kepler-Server/build.gradle Kepler-Server/build.gradle

# Fix line endings, make executable, and cache Gradle dependencies
RUN sed -i 's/\r$//' gradlew && \
    chmod +x gradlew && \
    ./gradlew --no-daemon dependencies

# Copy the rest of the source code
COPY . .

# Build and prepare the distribution
RUN sed -i 's/\r$//' tools/scripts/run.sh && \
    ./gradlew --no-daemon distZip && \
    unzip -qq ./Kepler-Server/build/distributions/Kepler-Server.zip -d ./release && \
    rm -rf ./release/Kepler-Server/bin && \
    mkdir -p ./build/lib && \
    mv ./release/Kepler-Server/lib/Kepler-Server.jar ./build/kepler.jar && \
    mv ./release/Kepler-Server/lib/* ./build/lib && \
    cp tools/scripts/run.sh ./build/

####################
# PRODUCTION STAGE #
####################

FROM alpine:3.23

RUN apk add --no-cache openjdk21-jre-headless && \
    addgroup -S kepler && adduser -S kepler -G kepler && \
    mkdir -p /kepler /var/log/kepler && \
    chown kepler:kepler /kepler /var/log/kepler

# Persistent log directory consumed by log4j.properties via -Dkepler.log.dir.
# Bind-mount /opt/kepler-logs:/var/log/kepler in the swarm service so logs
# survive container replacement and are reachable from the host.
VOLUME ["/var/log/kepler"]

WORKDIR /kepler

COPY --from=build --chown=kepler:kepler /kepler/build ./
COPY --chown=kepler:kepler server.ini ./

USER kepler

HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=3 \
  CMD cat /proc/net/tcp /proc/net/tcp6 2>/dev/null | grep -q ":$(printf '%04X' ${SERVER_PORT:-12321}) "

CMD ["sh", "run.sh"]
